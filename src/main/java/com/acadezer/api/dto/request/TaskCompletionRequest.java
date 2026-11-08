package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record TaskCompletionRequest(
    @NotNull(message = "El estado de completado es obligatorio")
    Boolean completado
) {}
