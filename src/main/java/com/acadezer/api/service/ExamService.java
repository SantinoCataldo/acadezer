package com.acadezer.api.service;

import com.acadezer.api.dto.request.ExamRequest;
import com.acadezer.api.dto.response.ExamResponse;
import com.acadezer.api.model.ExamEntity;
import com.acadezer.api.mapper.ExamMapper;
import com.acadezer.api.repository.ExamRepository;
import com.acadezer.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamService {
    private final ExamRepository repository;
    private final SubjectService subjects;
    private final ExamMapper mapper;

    public ExamEntity owned(Long id, String email) {
        return repository.findByIdAndMateriaUsuarioEmail(id, email).orElseThrow(ApiException::notFound);
    }

    public ExamResponse get(Long id, String email) {
        return mapper.toResponse(owned(id, email));
    }

    public List<ExamResponse> list(Long materiaId, String email) {
        subjects.owned(materiaId, email);
        return repository.findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(materiaId, email)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public ExamResponse create(Long materiaId, ExamRequest request, String email) {
        // Resolve ownership before mapping or saving client data.
        var subject = subjects.owned(materiaId, email);
        ExamEntity entity = mapper.toEntity(request);
        entity.setMateria(subject);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public ExamResponse update(Long id, ExamRequest request, String email) {
        ExamEntity entity = owned(id, email);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        repository.delete(owned(id, email));
    }

}
