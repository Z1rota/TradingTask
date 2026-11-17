package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.entity.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o.instrument.seccode from Order o where o.id = :id and o.user.id = :userId")
    Optional<String> findSeccodeByIdAndUserId(long id, long userId);

    @EntityGraph(attributePaths = {"account", "instrument"})
    Optional<Order> findByIdAndUserId(long id, long userId);

    @EntityGraph(attributePaths = {"account", "instrument"})
    Optional<Order> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey);

    @EntityGraph(attributePaths = {"account", "instrument"})
    @Query("""
            select o from Order o
            where o.instrument.id = :instrumentId and o.side = :side and o.status in :statuses
              and o.remainingQuantity > 0
              and ((:buy = true and o.limitPrice <= :price) or (:buy = false and o.limitPrice >= :price))
            """)
    List<Order> findMatchingOrders(long instrumentId, OrderSide side, List<OrderStatus> statuses,
                                   boolean buy, BigDecimal price, Sort sort);

    @Modifying
    @Query(value = """
            insert into orders (user_id, account_id, instrument_id, side, limit_price, initial_quantity,
                                remaining_quantity, status, created_at, idempotency_key, request_data)
            values (:userId, :accountId, :instrumentId, :side, :limitPrice, :quantity,
                    :quantity, 'NEW', clock_timestamp(), :key, :requestData)
            on conflict (user_id, idempotency_key) do nothing
            """, nativeQuery = true)
    int tryInsert(long userId, long accountId, long instrumentId, String side,
                  BigDecimal limitPrice, long quantity, String key, String requestData);
}
