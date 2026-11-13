package com.acadezer.api.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;

public record ExamResponse(
        Long id,
        String nombre,
        LocalDate fecha,
        BigDecimal calificacion,
        Long materiaId) {
}
