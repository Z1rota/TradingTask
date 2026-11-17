package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Account;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(long userId, Sort sort);

    Optional<Account> findByIdAndUserId(long id, long userId);

    @Modifying
    @Query("""
            update Account a set a.balance = a.balance + :amount
            where a.id = :id and a.balance <= :maxBalance - :amount
            """)
    int addBalance(@Param("id") long id, @Param("amount") BigDecimal amount, @Param("maxBalance") BigDecimal maxBalance);
}
