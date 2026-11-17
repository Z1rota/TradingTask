package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.order.CreateOrderRequest;
import dev.zirota.tradingtask.dto.order.OrderResponse;
import dev.zirota.tradingtask.dto.order.ReplaceOrderRequest;
import dev.zirota.tradingtask.entity.Account;
import dev.zirota.tradingtask.entity.Instrument;
import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.entity.OrderStatus;
import dev.zirota.tradingtask.repository.AccountRepository;
import dev.zirota.tradingtask.repository.InstrumentRepository;
import dev.zirota.tradingtask.repository.OrderRepository;
import dev.zirota.tradingtask.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;
    private final SettlementService settlementService;

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(long userId, String seccode, Long accountId, OrderStatus status,
                                        Pageable pageable) {
        return orderRepository.findRegistry(userId, seccode == null ? null : seccode.toUpperCase(),
                accountId, status, pageable).map(OrderResponse::from);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse create(long userId, String key, CreateOrderRequest request) {

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Instrument instrument = instrumentRepository.findBySeccodeForUpdate(request.seccode().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        return submit(userId, key, request, account, instrument, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse replace(long userId, long orderId, String key, ReplaceOrderRequest request) {

        String seccode = orderRepository.findSeccodeByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Instrument instrument = instrumentRepository.findBySeccodeForUpdate(seccode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        CreateOrderRequest replacement = new CreateOrderRequest(order.getAccount().getId(), seccode,
                order.getSide(), request.limitPrice(), request.quantity());
        return submit(userId, key, replacement, order.getAccount(), instrument, order);
    }

    private OrderResponse submit(long userId, String key, CreateOrderRequest request,
                                 Account account, Instrument instrument, Order replaced) {
        String requestData = (replaced == null ? "" : "replace:" + replaced.getId() + ":")
                + request.normalizedRequest();
        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return existingResponse(existing.get(), requestData);
        }
        if (replaced != null && replaced.getStatus() != OrderStatus.NEW
                && replaced.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active orders can be replaced");
        }
        if (!account.getCurrency().equals(instrument.getQuoteCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account and instrument currencies must match");
        }
        if (request.limitPrice().remainder(instrument.getMinPriceStep()).signum() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be a multiple of the minimum price step");
        }
        if (request.quantity() > Long.MAX_VALUE / instrument.getLotSize()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity in units is too large");
        }
        long units = request.quantity() * instrument.getLotSize();

        var candidates = matchingEngine.findCandidates(instrument.getId(), request.side(), request.limitPrice());

        var accountIds = Stream.concat(Stream.of(account.getId()),
                candidates.stream().map(candidate -> candidate.getAccount().getId())).distinct().toList();
        accountRepository.lockAllById(accountIds);

        int inserted = orderRepository.tryInsert(userId, account.getId(), instrument.getId(), request.side().name(),
                request.limitPrice(), request.quantity(), key, requestData);
        Order order = orderRepository.findByUserIdAndIdempotencyKey(userId, key).orElseThrow();
        if (inserted == 0) {
            return existingResponse(order, requestData);
        }
        if (replaced != null) {
            settlementService.releaseRemainder(replaced);
            replaced.cancelRemaining();
        }
        if (request.side() == OrderSide.BUY) {
            BigDecimal amount = request.limitPrice().multiply(BigDecimal.valueOf(units));
            if (accountRepository.reserveBalance(account.getId(), amount) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available balance");
            }
        } else if (positionRepository.reserveQuantity(account.getId(), instrument.getId(), units) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available quantity");
        }
        matchingEngine.match(order, candidates);
        return OrderResponse.from(order);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(long userId, long orderId) {
        String seccode = orderRepository.findSeccodeByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        instrumentRepository.findBySeccodeForUpdate(seccode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.FILLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Filled order cannot be cancelled");
        }

        accountRepository.lockAllById(List.of(order.getAccount().getId()));
        settlementService.releaseRemainder(order);
        order.cancelRemaining();
    }

    private OrderResponse existingResponse(Order order, String requestData) {
        if (!order.getRequestData().equals(requestData)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key has different parameters");
        }
        return OrderResponse.from(order);
    }
}
