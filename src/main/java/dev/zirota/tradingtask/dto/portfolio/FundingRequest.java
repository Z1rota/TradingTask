package dev.zirota.tradingtask.dto.portfolio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FundingRequest(
        @NotNull @Positive Long accountId,
        @Positive @Digits(integer = 20, fraction = 8) BigDecimal amount,
        @Pattern(regexp = "[a-zA-Z0-9]{1,32}") String seccode,
        @Positive Long quantity
) {

    @AssertTrue(message = "provide either amount or both seccode and quantity")
    @JsonIgnore
    public boolean isValidFunding() {
        return amount != null ? seccode == null && quantity == null : seccode != null && quantity != null;
    }

    public String normalizedRequest() {
        return accountId + ":" + (amount == null ? "" : amount.stripTrailingZeros().toPlainString())
                + ":" + (seccode == null ? "" : seccode.toUpperCase())
                + ":" + (quantity == null ? "" : quantity);
    }
}
