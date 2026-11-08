package com.acadezer.api.dto.response;

public record NoteResponse(
        Long id,
        String titulo,
        String contenido,
        Long materiaId) {
}
