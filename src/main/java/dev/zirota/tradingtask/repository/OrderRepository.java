package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"account", "instrument"})
    Optional<Order> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey);

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
