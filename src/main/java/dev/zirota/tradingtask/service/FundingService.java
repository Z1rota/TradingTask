package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.portfolio.FundingRequest;
import dev.zirota.tradingtask.entity.Account;
import dev.zirota.tradingtask.entity.FundingOperation;
import dev.zirota.tradingtask.entity.Instrument;
import dev.zirota.tradingtask.repository.AccountRepository;
import dev.zirota.tradingtask.repository.FundingOperationRepository;
import dev.zirota.tradingtask.repository.InstrumentRepository;
import dev.zirota.tradingtask.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final InstrumentRepository instrumentRepository;
    private final FundingOperationRepository fundingOperationRepository;

    @Transactional
    public void fund(long userId, String key, FundingRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        String requestData = request.normalizedRequest();
        if (fundingOperationRepository.tryInsert(userId, key, requestData) == 0) {
            FundingOperation previous = fundingOperationRepository.findByUserIdAndIdempotencyKey(userId, key).orElseThrow();
            if (!previous.getRequestData().equals(requestData)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid Idempotency Key");
            }
            return;
        }
        if (request.amount() != null) {
            if (accountRepository.addBalance(account.getId(), request.amount(), Account.MAX_BALANCE) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum account balance exceeded");
            }
            return;
        }
        Instrument instrument = instrumentRepository.findBySeccodeForUpdate(request.seccode().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        if (!account.getCurrency().equals(instrument.getQuoteCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account and instrument currencies must match");
        }
        if (positionRepository.addQuantity(account.getId(), instrument.getId(), request.quantity()) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum position quantity exceeded");
        }
    }
}
