package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.ExamRequest;
import com.acadezer.api.dto.response.ExamResponse;
import com.acadezer.api.model.ExamEntity;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {
    public ExamEntity toEntity(ExamRequest request) {
        ExamEntity entity = new ExamEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(ExamEntity entity, ExamRequest request) {
        entity.setNombre(request.nombre());
        entity.setFecha(request.fecha());
        entity.setCalificacion(request.calificacion());
    }

    public ExamResponse toResponse(ExamEntity entity) {
        return new ExamResponse(entity.getId(), entity.getNombre(), entity.getFecha(), entity.getCalificacion(),
                entity.getMateria().getId());
    }
}
