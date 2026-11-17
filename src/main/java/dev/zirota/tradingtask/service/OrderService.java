package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.order.CreateOrderRequest;
import dev.zirota.tradingtask.dto.order.OrderResponse;
import dev.zirota.tradingtask.entity.Account;
import dev.zirota.tradingtask.entity.Instrument;
import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.repository.AccountRepository;
import dev.zirota.tradingtask.repository.InstrumentRepository;
import dev.zirota.tradingtask.repository.OrderRepository;
import dev.zirota.tradingtask.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse create(long userId, String key, CreateOrderRequest request) {

        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Instrument instrument = instrumentRepository.findBySeccodeForUpdate(request.seccode().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        String requestData = request.normalizedRequest();
        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return existingResponse(existing.get(), requestData);
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

    private OrderResponse existingResponse(Order order, String requestData) {
        if (!order.getRequestData().equals(requestData)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key has different parameters");
        }
        return OrderResponse.from(order);
    }
}
