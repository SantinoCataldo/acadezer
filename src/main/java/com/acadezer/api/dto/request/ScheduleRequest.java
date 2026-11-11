package com.acadezer.api.dto.request;

import com.acadezer.api.enums.Weekday;
import jakarta.validation.constraints.*;
import java.time.LocalTime;

public record ScheduleRequest(
    Long id,
    @NotNull Weekday diaSemana,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @NotNull @Min(1) @Max(2) Integer controlesAsistencia
) {}
