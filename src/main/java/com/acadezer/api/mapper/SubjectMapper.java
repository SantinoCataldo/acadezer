package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.SubjectRequest;
import com.acadezer.api.dto.response.SubjectResponse;
import com.acadezer.api.model.SubjectEntity;
import org.springframework.stereotype.Component;
import com.acadezer.api.dto.response.ScheduleResponse;
import java.math.BigDecimal;

@Component
public class SubjectMapper {
    public SubjectEntity toEntity(SubjectRequest request) {
        SubjectEntity entity = new SubjectEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(SubjectEntity entity, SubjectRequest request) {
        entity.setNombre(request.nombre().trim());
        entity.setFechaInicio(request.fechaInicio());
        entity.setFechaFin(request.fechaFin());
        entity.setPorcentajeAsistenciaMinima(request.porcentajeAsistenciaMinima() == null ? new BigDecimal("75.00") : request.porcentajeAsistenciaMinima());
    }

    public SubjectResponse toResponse(SubjectEntity entity) {
        return new SubjectResponse(entity.getId(), entity.getNombre(), entity.getFechaInicio(), entity.getFechaFin(),
            entity.getPorcentajeAsistenciaMinima(), entity.getHorarios().stream()
                .map(h -> new ScheduleResponse(h.getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin(), h.getControlesAsistencia())).toList());
    }
}
