package dev.zirota.tradingtask.dto.order;

import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id, Long accountId, String seccode, OrderSide side, BigDecimal limitPrice,
        long initialQuantity, long remainingQuantity, OrderStatus status, Instant createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getAccount().getId(), order.getInstrument().getSeccode(),
                order.getSide(), order.getLimitPrice(), order.getInitialQuantity(), order.getRemainingQuantity(),
                order.getStatus(), order.getCreatedAt());
    }
}
