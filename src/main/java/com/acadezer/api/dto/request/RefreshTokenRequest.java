package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    @Size(max = 128)
    String refreshToken
) {}
