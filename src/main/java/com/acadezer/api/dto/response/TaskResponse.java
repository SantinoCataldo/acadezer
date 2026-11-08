package com.acadezer.api.dto.response;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String titulo,
        String descripcion,
        LocalDate fechaLimite,
        boolean completado,
        Long materiaId) {
}
