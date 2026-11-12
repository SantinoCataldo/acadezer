package com.acadezer.api.dto.response;

import com.acadezer.api.enums.ExamStatus;
import com.acadezer.api.enums.ExamType;
import java.time.LocalDate;
import java.math.BigDecimal;

public record ExamResponse(
        Long id,
        String nombre,
        LocalDate fecha,
        ExamType tipo,
        ExamStatus estado,
        BigDecimal calificacion,
        Long materiaId) {
}
