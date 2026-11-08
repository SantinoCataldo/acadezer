package com.acadezer.api.service;

import com.acadezer.api.dto.request.NoteRequest;
import com.acadezer.api.dto.response.NoteResponse;
import com.acadezer.api.model.NoteEntity;
import com.acadezer.api.mapper.NoteMapper;
import com.acadezer.api.repository.NoteRepository;
import com.acadezer.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {
    private final NoteRepository repository;
    private final SubjectService subjects;
    private final NoteMapper mapper;

    public NoteEntity owned(Long id, String email) {
        return repository.findByIdAndMateriaUsuarioEmail(id, email).orElseThrow(ApiException::notFound);
    }

    public NoteResponse get(Long id, String email) {
        return mapper.toResponse(owned(id, email));
    }

    public List<NoteResponse> list(Long materiaId, String email) {
        subjects.owned(materiaId, email);
        return repository.findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(materiaId, email)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(Long materiaId, NoteRequest request, String email) {
        // Resolve ownership before mapping or saving client data.
        var subject = subjects.owned(materiaId, email);
        NoteEntity entity = mapper.toEntity(request);
        entity.setMateria(subject);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public NoteResponse update(Long id, NoteRequest request, String email) {
        NoteEntity entity = owned(id, email);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        repository.delete(owned(id, email));
    }

}
