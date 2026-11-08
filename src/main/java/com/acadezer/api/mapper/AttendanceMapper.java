package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.AttendanceRequest;
import com.acadezer.api.dto.response.AttendanceResponse;
import com.acadezer.api.model.AttendanceEntity;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {
    public AttendanceEntity toEntity(AttendanceRequest request) {
        AttendanceEntity entity = new AttendanceEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(AttendanceEntity entity, AttendanceRequest request) {
        entity.setFecha(request.fecha());
        entity.setEstado(request.estado());
    }

    public AttendanceResponse toResponse(AttendanceEntity entity) {
        return new AttendanceResponse(entity.getId(), entity.getFecha(), entity.getEstado(),
                entity.getMateria().getId());
    }
}
