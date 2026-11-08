package com.acadezer.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import com.acadezer.api.enums.AttendanceStatus;

public record AttendanceRequest(
    @NotNull(message = "La fecha de clase es obligatoria")
    LocalDate fecha,

    @NotNull(message = "El estado de asistencia es obligatorio")
    AttendanceStatus estado
) {}
