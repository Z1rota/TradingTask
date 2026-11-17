package dev.zirota.tradingtask.dto.trade;

import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.entity.Trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        Long id, Long orderId, Long accountId, String seccode, OrderSide side,
        BigDecimal price, long quantity, Instant executedAt
) {

    public static TradeResponse from(Trade trade, long userId) {
        Order order = trade.getBuyOrder().getUser().getId().equals(userId)
                ? trade.getBuyOrder() : trade.getSellOrder();
        return new TradeResponse(trade.getId(), order.getId(), order.getAccount().getId(),
                trade.getInstrument().getSeccode(), order.getSide(), trade.getPrice(),
                trade.getQuantity(), trade.getExecutedAt());
    }
}
