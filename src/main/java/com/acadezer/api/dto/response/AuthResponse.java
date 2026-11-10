package com.acadezer.api.dto.response;

public record AuthResponse(
        String accessToken,
        @com.fasterxml.jackson.annotation.JsonIgnore String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse usuario) {
}
