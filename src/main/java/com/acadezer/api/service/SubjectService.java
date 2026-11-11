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
import java.util.HashSet;
import com.acadezer.api.dto.request.ScheduleRequest;
import com.acadezer.api.model.ScheduleEntity;

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
    private final AttendanceControlRepository controls;
    private final ExcludedDateRepository excludedDates;

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
        validate(request);
        SubjectEntity entity = mapper.toEntity(request);
        entity.setUsuario(users.findByEmail(email).orElseThrow(ApiException::notFound));
        applySchedules(entity, request.horarios());
        return mapper.toResponse(subjects.save(entity));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request, String email) {
        SubjectEntity entity = owned(id, email);
        validate(request);
        if (controls.countOutsideDates(id, request.fechaInicio(), request.fechaFin()) > 0)
            throw new ApiException(409, "Las fechas dejarían controles registrados fuera de la materia");
        applySchedules(entity, request.horarios());
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
        controls.deleteByHorarioMateriaId(id);
        excludedDates.deleteByMateriaId(id);
        subjects.delete(entity);
    }

    private void validate(SubjectRequest request) {
        if (request.fechaFin().isBefore(request.fechaInicio())) throw new ApiException(400, "La fecha de fin debe ser igual o posterior a la de inicio");
        for (int i = 0; i < request.horarios().size(); i++) {
            ScheduleRequest a = request.horarios().get(i);
            if (!a.horaInicio().isBefore(a.horaFin())) throw new ApiException(400, "La hora de fin debe ser posterior a la de inicio");
            for (int j = 0; j < i; j++) {
                ScheduleRequest b = request.horarios().get(j);
                if (a.diaSemana() == b.diaSemana() && a.horaInicio().isBefore(b.horaFin()) && b.horaInicio().isBefore(a.horaFin()))
                    throw new ApiException(400, "Los horarios de un mismo día no pueden superponerse");
            }
        }
    }

    private void applySchedules(SubjectEntity subject, List<ScheduleRequest> requests) {
        var existing = new java.util.HashMap<Long, ScheduleEntity>();
        subject.getHorarios().forEach(h -> existing.put(h.getId(), h));
        var retained = new HashSet<Long>();
        for (ScheduleRequest request : requests) {
            ScheduleEntity schedule;
            if (request.id() == null) {
                schedule = new ScheduleEntity(); schedule.setMateria(subject); subject.getHorarios().add(schedule);
            } else {
                schedule = existing.get(request.id());
                if (schedule == null) throw ApiException.notFound();
                if (!retained.add(request.id())) throw new ApiException(400, "Un horario no puede aparecer dos veces");
                boolean changed = schedule.getDiaSemana() != request.diaSemana() || !schedule.getHoraInicio().equals(request.horaInicio())
                    || !schedule.getHoraFin().equals(request.horaFin()) || schedule.getControlesAsistencia() != request.controlesAsistencia();
                if (changed && controls.existsByHorarioId(schedule.getId())) throw new ApiException(409, "El horario tiene controles registrados y no puede modificarse");
            }
            schedule.setDiaSemana(request.diaSemana()); schedule.setHoraInicio(request.horaInicio());
            schedule.setHoraFin(request.horaFin()); schedule.setControlesAsistencia(request.controlesAsistencia());
        }
        for (ScheduleEntity schedule : existing.values()) {
            if (!retained.contains(schedule.getId()) && controls.existsByHorarioId(schedule.getId()))
                throw new ApiException(409, "El horario tiene controles registrados y no puede eliminarse");
        }
        subject.getHorarios().removeIf(h -> h.getId() != null && !retained.contains(h.getId()));
    }
}
