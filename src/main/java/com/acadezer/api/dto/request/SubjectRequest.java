package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubjectRequest(
    @NotBlank(message = "El nombre de la materia es obligatorio")
    @Size(max = 150)
    String nombre,

    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin,
    @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal porcentajeAsistenciaMinima,
    @NotNull @Size(max = 50) List<@NotNull @Valid ScheduleRequest> horarios
) {}
