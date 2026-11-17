package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Position;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @EntityGraph(attributePaths = {"instrument", "account"})
    List<Position> findAllByAccountUserId(long userId, Sort sort);

    @Modifying
    @Query(value = """
            insert into positions (account_id, instrument_id, quantity, reserved_quantity)
            values (:accountId, :instrumentId, :quantity, 0)
            on conflict (account_id, instrument_id) do update
            set quantity = positions.quantity + excluded.quantity
            where positions.quantity <= 9223372036854775807 - excluded.quantity
            """, nativeQuery = true)
    int addQuantity(long accountId, long instrumentId, long quantity);

    @Modifying
    @Query("""
            update Position p set p.reservedQuantity = p.reservedQuantity + :quantity
            where p.account.id = :accountId and p.instrument.id = :instrumentId
              and p.quantity - p.reservedQuantity >= :quantity
            """)
    int reserveQuantity(long accountId, long instrumentId, long quantity);

    @Modifying
    @Query("""
            update Position p set p.quantity = p.quantity - :quantity, p.reservedQuantity = p.reservedQuantity - :quantity
            where p.account.id = :accountId and p.instrument.id = :instrumentId
              and p.quantity >= :quantity and p.reservedQuantity >= :quantity
            """)
    int settleSell(long accountId, long instrumentId, long quantity);

    @Modifying
    @Query("""
            update Position p set p.reservedQuantity = p.reservedQuantity - :quantity
            where p.account.id = :accountId and p.instrument.id = :instrumentId and p.reservedQuantity >= :quantity
            """)
    int releaseQuantity(long accountId, long instrumentId, long quantity);
}
