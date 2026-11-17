package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.FundingOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FundingOperationRepository extends JpaRepository<FundingOperation, Long> {

    @Modifying
    @Query(value = """
            insert into funding_operations (user_id, idempotency_key, request_data)
            values (:userId, :key, :requestData)
            on conflict (user_id, idempotency_key) do nothing
            """, nativeQuery = true)
    int tryInsert(@Param("userId") long userId, @Param("key") String key, @Param("requestData") String requestData);

    Optional<FundingOperation> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey);
}
