package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(
    @NotNull(message = "El estado de habilitación es obligatorio")
    Boolean enabled
) {}
