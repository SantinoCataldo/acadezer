package com.acadezer.api.service;

import com.acadezer.api.dto.request.SubjectRequest;
import com.acadezer.api.dto.response.SubjectResponse;
import com.acadezer.api.model.SubjectEntity;
import com.acadezer.api.mapper.SubjectMapper;
import com.acadezer.api.repository.*;
import com.acadezer.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository subjects;
    private final UserRepository users;
    private final NoteRepository notes;
    private final TaskRepository tasks;
    private final ExamRepository exams;
    private final AttendanceRepository attendance;
    private final SubjectMapper mapper;

    public SubjectEntity owned(Long id, String email) {
        return subjects.findByIdAndUsuarioEmail(id, email).orElseThrow(ApiException::notFound);
    }

    public List<SubjectResponse> list(String email) {
        return subjects.findByUsuarioEmailOrderByIdAsc(email).stream().map(mapper::toResponse).toList();
    }

    public SubjectResponse get(Long id, String email) {
        return mapper.toResponse(owned(id, email));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request, String email) {
        SubjectEntity entity = mapper.toEntity(request);
        entity.setUsuario(users.findByEmail(email).orElseThrow(ApiException::notFound));
        return mapper.toResponse(subjects.save(entity));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request, String email) {
        SubjectEntity entity = owned(id, email);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id, String email) {
        SubjectEntity entity = owned(id, email);
        notes.deleteByMateriaId(id);
        tasks.deleteByMateriaId(id);
        exams.deleteByMateriaId(id);
        attendance.deleteByMateriaId(id);
        subjects.delete(entity);
    }
}
