package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
    @NotBlank(message = "El título del apunte es obligatorio")
    @Size(max = 200)
    String titulo,

    @NotBlank(message = "El contenido del apunte es obligatorio")
    @Size(max = 20000)
    String contenido
) {}
