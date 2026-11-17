package dev.zirota.tradingtask.dto.order;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReplaceOrderRequest(
        @NotNull @Positive @Digits(integer = 11, fraction = 8) BigDecimal limitPrice,
        @NotNull @Positive Long quantity
) {
}
