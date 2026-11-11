package com.acadezer.api.dto.response;

import java.time.Instant;

public record NoteResponse(
        Long id,
        String titulo,
        String contenido,
        Long materiaId,
        Instant creadoEn,
        Instant actualizadoEn) {
}
