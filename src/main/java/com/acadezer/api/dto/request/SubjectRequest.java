package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
    @NotBlank(message = "El nombre de la materia es obligatorio")
    @Size(max = 150)
    String nombre,

    @Size(max = 2000)
    String descripcion
) {}
