package com.acadezer.api.dto.response;
import java.math.BigDecimal;
import java.util.List;
public record AttendanceSummaryResponse(int presentes, int ausentes, int justificadas, int pendientes,
        int controlesMarcados, BigDecimal porcentajeAsistencia, List<AttendanceControlResponse> controles) {}
