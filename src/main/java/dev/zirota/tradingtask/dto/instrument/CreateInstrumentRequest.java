package dev.zirota.tradingtask.dto.instrument;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateInstrumentRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9]{1,32}", message = "must contain 1 to 32 latin letters or digits")
        String seccode,
        @NotNull @Positive Integer lotSize,
        @NotNull @Positive @Digits(integer = 11, fraction = 8) BigDecimal minPriceStep,
        @NotNull Currency quoteCurrency
) {
}
