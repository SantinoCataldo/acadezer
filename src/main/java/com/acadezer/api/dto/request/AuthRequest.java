package com.acadezer.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 254)
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72)
    String password
) {}
