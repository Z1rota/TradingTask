package dev.zirota.tradingtask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_user_key", columnNames = {"user_id", "idempotency_key"}))
@Check(constraints = "limit_price > 0 and initial_quantity > 0 and remaining_quantity >= 0 and remaining_quantity <= initial_quantity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal limitPrice;

    @Column(nullable = false)
    private long initialQuantity;

    @Column(nullable = false)
    private long remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 128)
    private String requestData;

    public void fill(long quantity) {
        if (quantity <= 0 || quantity > remainingQuantity
                || (status != OrderStatus.NEW && status != OrderStatus.PARTIALLY_FILLED)) {
            throw new IllegalStateException("Order cannot execute this quantity");
        }
        remainingQuantity -= quantity;
        status = remainingQuantity == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public void cancelRemaining() {
        if (remainingQuantity <= 0 || (status != OrderStatus.NEW && status != OrderStatus.PARTIALLY_FILLED)) {
            throw new IllegalStateException("Order has no active remainder");
        }
        status = OrderStatus.CANCELLED;
    }
}
