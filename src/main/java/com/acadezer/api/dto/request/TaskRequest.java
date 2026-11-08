package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank(message = "El título del pendiente es obligatorio")
    @Size(max = 200)
    String titulo,

    @Size(max = 2000)
    String descripcion,

    LocalDate fechaLimite
) {}
