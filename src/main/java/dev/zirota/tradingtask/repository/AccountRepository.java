package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Account;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(long userId, Sort sort);
}
