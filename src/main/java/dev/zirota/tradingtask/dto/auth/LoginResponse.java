package dev.zirota.tradingtask.dto.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
