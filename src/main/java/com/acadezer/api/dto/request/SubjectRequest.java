package com.acadezer.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record SubjectRequest(
    @NotBlank(message = "El nombre de la materia es obligatorio")
    @Size(max = 150)
    String nombre,

    @NotNull LocalDate fechaInicio,
    LocalDate fechaFin,
    @NotNull @Size(max = 50) List<@NotNull @Valid ScheduleRequest> horarios
) {}
