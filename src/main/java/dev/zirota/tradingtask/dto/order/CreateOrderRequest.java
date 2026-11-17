package dev.zirota.tradingtask.dto.order;

import dev.zirota.tradingtask.entity.OrderSide;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull @Positive Long accountId,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9]{1,32}") String seccode,
        @NotNull OrderSide side,
        @NotNull @Positive @Digits(integer = 11, fraction = 8) BigDecimal limitPrice,
        @NotNull @Positive Long quantity
) {

    public String normalizedRequest() {
        return accountId + ":" + seccode.toUpperCase() + ":" + side
                + ":" + limitPrice.stripTrailingZeros().toPlainString() + ":" + quantity;
    }
}
