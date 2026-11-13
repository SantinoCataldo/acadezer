package com.acadezer.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.math.BigDecimal;

public record ExamRequest(
    @NotBlank(message = "El nombre de la evaluación es obligatorio")
    @Size(max = 200)
    String nombre,

    @NotNull(message = "La fecha de la evaluación es obligatoria")
    LocalDate fecha,

    @DecimalMin(value = "0", message = "La calificación debe ser mayor o igual a 0")
    @DecimalMax(value = "10", message = "La calificación debe ser menor o igual a 10")
    @Digits(integer = 2, fraction = 2, message = "La calificación admite hasta dos decimales")
    BigDecimal calificacion
) {}
