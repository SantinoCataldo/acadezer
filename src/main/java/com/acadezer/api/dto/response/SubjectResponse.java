package com.acadezer.api.dto.response;

import java.time.LocalDate;
import java.util.List;

public record SubjectResponse(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<ScheduleResponse> horarios) {
}
