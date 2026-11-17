package dev.zirota.tradingtask.dto.portfolio;

import jakarta.validation.constraints.NotNull;

import java.util.Currency;

public record CreateAccountRequest(@NotNull Currency currency) {
}
