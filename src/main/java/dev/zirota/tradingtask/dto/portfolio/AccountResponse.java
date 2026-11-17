package dev.zirota.tradingtask.dto.portfolio;

import dev.zirota.tradingtask.entity.Account;

import java.math.BigDecimal;

public record AccountResponse(
        Long id, String currency, BigDecimal balance, BigDecimal reservedBalance, BigDecimal availableBalance
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getCurrency(), account.getBalance(),
                account.getReservedBalance(), account.getBalance().subtract(account.getReservedBalance()));
    }
}
