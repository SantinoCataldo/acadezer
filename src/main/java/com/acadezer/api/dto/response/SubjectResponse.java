package com.acadezer.api.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record SubjectResponse(
        Long id,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal porcentajeAsistenciaMinima,
        List<ScheduleResponse> horarios) {
}
