package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @EntityGraph(attributePaths = {"instrument", "buyOrder", "sellOrder"})
    @Query("""
            select t from Trade t
            where ((t.buyOrder.user.id = :userId and (:accountId is null or t.buyOrder.account.id = :accountId))
                or (t.sellOrder.user.id = :userId and (:accountId is null or t.sellOrder.account.id = :accountId)))
              and (:seccode is null or t.instrument.seccode = :seccode)
            """)
    Page<Trade> findRegistry(long userId, String seccode, Long accountId, Pageable pageable);
}
