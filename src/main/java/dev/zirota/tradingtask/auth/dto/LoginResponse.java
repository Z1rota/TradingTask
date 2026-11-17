package dev.zirota.tradingtask.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
