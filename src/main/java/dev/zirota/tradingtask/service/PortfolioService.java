package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.portfolio.AccountResponse;
import dev.zirota.tradingtask.dto.portfolio.CreateAccountRequest;
import dev.zirota.tradingtask.dto.portfolio.PositionResponse;
import dev.zirota.tradingtask.entity.Account;
import dev.zirota.tradingtask.entity.User;
import dev.zirota.tradingtask.repository.AccountRepository;
import dev.zirota.tradingtask.repository.PositionRepository;
import dev.zirota.tradingtask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String currency = request.currency().getCurrencyCode();
        return AccountResponse.from(accountRepository.save(new Account(user, currency)));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAccounts(long userId) {
        return accountRepository.findAllByUserId(userId, Sort.by("currency", "id")).stream()
                .map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> findPositions(long userId) {
        return positionRepository.findAllByAccountUserId(userId, Sort.by("account.id", "instrument.seccode")).stream()
                .map(PositionResponse::from).toList();
    }
}
