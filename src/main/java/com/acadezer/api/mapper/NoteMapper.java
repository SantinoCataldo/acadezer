package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.NoteRequest;
import com.acadezer.api.dto.response.NoteResponse;
import com.acadezer.api.model.NoteEntity;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {
    public NoteEntity toEntity(NoteRequest request) {
        NoteEntity entity = new NoteEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(NoteEntity entity, NoteRequest request) {
        entity.setTitulo(request.titulo());
        entity.setContenido(request.contenido());
    }

    public NoteResponse toResponse(NoteEntity entity) {
        return new NoteResponse(
                entity.getId(),
                entity.getTitulo(),
                entity.getContenido(),
                entity.getMateria().getId(),
                entity.getCreadoEn(),
                entity.getActualizadoEn());
    }
}
