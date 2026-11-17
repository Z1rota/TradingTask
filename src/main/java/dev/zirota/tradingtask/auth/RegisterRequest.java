package dev.zirota.tradingtask.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9]{3,32}", message = "must contain 3 to 32 latin letters or digits")
        String username,
        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
