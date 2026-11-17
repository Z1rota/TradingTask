package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.entity.OrderStatus;
import dev.zirota.tradingtask.entity.Trade;
import dev.zirota.tradingtask.repository.OrderRepository;
import dev.zirota.tradingtask.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MatchingEngine {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final SettlementService settlementService;

    public List<Order> findCandidates(long instrumentId, OrderSide incomingSide, BigDecimal price) {
        boolean buy = incomingSide == OrderSide.BUY;
        Sort sort = Sort.by(buy ? Sort.Direction.ASC : Sort.Direction.DESC, "limitPrice").and(Sort.by("id"));
        return orderRepository.findMatchingOrders(instrumentId, buy ? OrderSide.SELL : OrderSide.BUY,
                List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED), buy, price, sort);
    }

    public void match(Order incoming, List<Order> candidates) {
        for (Order resting : candidates) {
            if (incoming.getRemainingQuantity() == 0) {
                break;
            }
            if (incoming.getUser().getId().equals(resting.getUser().getId())) {
                settlementService.releaseRemainder(incoming);
                incoming.cancelRemaining();
                break;
            }
            long lots = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());
            Order buy = incoming.getSide() == OrderSide.BUY ? incoming : resting;
            Order sell = incoming.getSide() == OrderSide.SELL ? incoming : resting;
            settlementService.settle(buy, sell, resting.getLimitPrice(), lots);
            buy.fill(lots);
            sell.fill(lots);
            tradeRepository.save(new Trade(buy, sell, resting.getLimitPrice(), lots));
        }
    }
}
