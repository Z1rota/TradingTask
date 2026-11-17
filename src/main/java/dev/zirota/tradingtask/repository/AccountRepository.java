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

    @Query(value = "select * from accounts where id in (:ids) order by id for no key update", nativeQuery = true)
    List<Account> lockAllById(List<Long> ids);

    @Modifying
    @Query("update Account a set a.balance = a.balance + :amount where a.id = :id and a.balance <= :maxBalance - :amount")
    int addBalance(long id, BigDecimal amount, BigDecimal maxBalance);

    @Modifying
    @Query("update Account a set a.reservedBalance = a.reservedBalance + :amount where a.id = :id and a.balance - a.reservedBalance >= :amount")
    int reserveBalance(long id, BigDecimal amount);

    @Modifying
    @Query("""
            update Account a set a.balance = a.balance - :payment, a.reservedBalance = a.reservedBalance - :released
            where a.id = :id and a.balance >= :payment and a.reservedBalance >= :released
            """)
    int settleBuy(long id, BigDecimal payment, BigDecimal released);

    @Modifying
    @Query("update Account a set a.reservedBalance = a.reservedBalance - :amount where a.id = :id and a.reservedBalance >= :amount")
    int releaseBalance(long id, BigDecimal amount);
}
