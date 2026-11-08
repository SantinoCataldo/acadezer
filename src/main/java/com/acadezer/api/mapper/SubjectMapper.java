package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.SubjectRequest;
import com.acadezer.api.dto.response.SubjectResponse;
import com.acadezer.api.model.SubjectEntity;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {
    public SubjectEntity toEntity(SubjectRequest request) {
        SubjectEntity entity = new SubjectEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(SubjectEntity entity, SubjectRequest request) {
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(request.descripcion());
    }

    public SubjectResponse toResponse(SubjectEntity entity) {
        return new SubjectResponse(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }
}
