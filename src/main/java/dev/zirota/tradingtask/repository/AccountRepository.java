package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Account;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
    int addBalance(long id, BigDecimal amount, BigDecimal maxBalance);

    @Modifying
    @Query("""
            update Account a set a.reservedBalance = a.reservedBalance + :amount
            where a.id = :id and a.balance - a.reservedBalance >= :amount
            """)
    int reserveBalance(long id, BigDecimal amount);
}
