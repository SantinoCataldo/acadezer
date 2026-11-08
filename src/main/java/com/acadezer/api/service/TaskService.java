package com.acadezer.api.service;

import com.acadezer.api.dto.request.TaskRequest;
import com.acadezer.api.dto.response.TaskResponse;
import com.acadezer.api.model.TaskEntity;
import com.acadezer.api.mapper.TaskMapper;
import com.acadezer.api.repository.TaskRepository;
import com.acadezer.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository repository;
    private final SubjectService subjects;
    private final TaskMapper mapper;

    public TaskEntity owned(Long id, String email) {
        return repository.findByIdAndMateriaUsuarioEmail(id, email).orElseThrow(ApiException::notFound);
    }

    public TaskResponse get(Long id, String email) {
        return mapper.toResponse(owned(id, email));
    }

    public List<TaskResponse> list(Long materiaId, String email) {
        subjects.owned(materiaId, email);
        return repository.findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(materiaId, email)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public TaskResponse create(Long materiaId, TaskRequest request, String email) {
        // Resolve ownership before mapping or saving client data.
        var subject = subjects.owned(materiaId, email);
        TaskEntity entity = mapper.toEntity(request);
        entity.setMateria(subject);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request, String email) {
        TaskEntity entity = owned(id, email);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        repository.delete(owned(id, email));
    }

    @Transactional
    public TaskResponse complete(Long id, boolean completed, String email) {
        TaskEntity entity = owned(id, email);
        entity.setCompletado(completed);
        return mapper.toResponse(entity);
    }

    public List<TaskResponse> upcoming(LocalDate desde, LocalDate hasta, String email) {
        if (hasta.isBefore(desde)) {
            throw new ApiException(400, "hasta debe ser igual o posterior a desde");
        }
        return repository
                .findByMateriaUsuarioEmailAndCompletadoFalseAndFechaLimiteBetweenOrderByFechaLimiteAscIdAsc(email,
                        desde, hasta)
                .stream().map(mapper::toResponse).toList();
    }

}
