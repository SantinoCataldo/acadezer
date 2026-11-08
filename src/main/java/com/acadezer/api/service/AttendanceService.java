package com.acadezer.api.service;

import com.acadezer.api.dto.request.AttendanceRequest;
import com.acadezer.api.dto.response.AttendanceResponse;
import com.acadezer.api.model.AttendanceEntity;
import com.acadezer.api.mapper.AttendanceMapper;
import com.acadezer.api.repository.AttendanceRepository;
import com.acadezer.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {
    private final AttendanceRepository repository;
    private final SubjectService subjects;
    private final AttendanceMapper mapper;

    public AttendanceEntity owned(Long id, String email) {
        return repository.findByIdAndMateriaUsuarioEmail(id, email).orElseThrow(ApiException::notFound);
    }

    public AttendanceResponse get(Long id, String email) {
        return mapper.toResponse(owned(id, email));
    }

    public List<AttendanceResponse> list(Long materiaId, String email) {
        subjects.owned(materiaId, email);
        return repository.findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(materiaId, email)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public AttendanceResponse create(Long materiaId, AttendanceRequest request, String email) {
        // Resolve ownership before mapping or saving client data.
        var subject = subjects.owned(materiaId, email);
        AttendanceEntity entity = mapper.toEntity(request);
        entity.setMateria(subject);
        checkDuplicate(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request, String email) {
        AttendanceEntity entity = owned(id, email);
        checkDuplicate(entity, request);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        repository.delete(owned(id, email));
    }

    private void checkDuplicate(AttendanceEntity entity, AttendanceRequest request) {
        Long excludedId = entity.getId() == null ? -1L : entity.getId();
        if (repository.existsByMateriaIdAndFechaAndIdNot(entity.getMateria().getId(), request.fecha(), excludedId)) {
            throw new ApiException(409, "Ya existe asistencia para esa materia y fecha");
        }
    }

}
