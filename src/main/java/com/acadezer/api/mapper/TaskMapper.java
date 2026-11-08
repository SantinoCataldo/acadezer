package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.TaskRequest;
import com.acadezer.api.dto.response.TaskResponse;
import com.acadezer.api.model.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    public TaskEntity toEntity(TaskRequest request) {
        TaskEntity entity = new TaskEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(TaskEntity entity, TaskRequest request) {
        entity.setTitulo(request.titulo());
        entity.setDescripcion(request.descripcion());
        entity.setFechaLimite(request.fechaLimite());
    }

    public TaskResponse toResponse(TaskEntity entity) {
        return new TaskResponse(entity.getId(), entity.getTitulo(), entity.getDescripcion(), entity.getFechaLimite(),
                entity.isCompletado(), entity.getMateria().getId());
    }
}
