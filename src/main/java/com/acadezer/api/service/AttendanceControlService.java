package com.acadezer.api.service;

import com.acadezer.api.dto.request.AttendanceControlRequest;
import com.acadezer.api.dto.response.*;
import com.acadezer.api.enums.*;
import com.acadezer.api.exception.ApiException;
import com.acadezer.api.model.*;
import com.acadezer.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AttendanceControlService {
    private final SubjectService subjects;
    private final ScheduleRepository schedules;
    private final AttendanceControlRepository controls;
    private final ExcludedDateRepository excludedDates;

    private SubjectEntity subject(Long id, String email) {
        SubjectEntity subject = subjects.owned(id, email);
        if (subject.getFechaInicio() == null)
            throw new ApiException(409, "Configure la fecha de inicio antes de usar los controles");
        return subject;
    }
    private ScheduleEntity schedule(Long subjectId, Long scheduleId, String email) {
        return schedules.findByIdAndMateriaIdAndMateriaUsuarioEmail(scheduleId, subjectId, email).orElseThrow(ApiException::notFound);
    }
    private void checkDate(SubjectEntity subject, ScheduleEntity schedule, LocalDate fecha, int numero) {
        LocalDate end = subject.getFechaFin() == null
            ? LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
            : subject.getFechaFin();
        if (fecha.isBefore(subject.getFechaInicio()) || fecha.isAfter(end) || Weekday.from(fecha.getDayOfWeek()) != schedule.getDiaSemana())
            throw new ApiException(400, "La fecha no corresponde a una clase del horario");
        if (numero < 1 || numero > schedule.getControlesAsistencia()) throw new ApiException(400, "Número de control inválido para el horario");
        if (excludedDates.findByMateriaIdAndFecha(subject.getId(), fecha).isPresent()) throw new ApiException(409, "La fecha está marcada sin cursada");
    }
    @Transactional
    public AttendanceControlResponse put(Long subjectId, Long scheduleId, LocalDate fecha, int numero, AttendanceControlRequest request, String email) {
        SubjectEntity subject = subject(subjectId, email);
        ScheduleEntity schedule = schedule(subjectId, scheduleId, email);
        checkDate(subject, schedule, fecha, numero);
        AttendanceControlEntity control = controls.findByHorarioIdAndFechaAndNumero(scheduleId, fecha, numero).orElseGet(AttendanceControlEntity::new);
        control.setHorario(schedule); control.setFecha(fecha); control.setNumero(numero); control.setEstado(request.estado());
        return response(schedule, fecha, numero, controls.saveAndFlush(control));
    }
    @Transactional
    public void delete(Long subjectId, Long scheduleId, LocalDate fecha, int numero, String email) {
        SubjectEntity subject = subject(subjectId, email);
        ScheduleEntity schedule = schedule(subjectId, scheduleId, email);
        checkDate(subject, schedule, fecha, numero);
        controls.delete(controls.findByHorarioIdAndFechaAndNumero(scheduleId, fecha, numero).orElseThrow(ApiException::notFound));
    }
    @Transactional
    public void exclude(Long subjectId, LocalDate fecha, String email) {
        SubjectEntity subject = subject(subjectId, email);
        if (fecha.isBefore(subject.getFechaInicio()) || (subject.getFechaFin() != null && fecha.isAfter(subject.getFechaFin())))
            throw new ApiException(400, "La fecha está fuera de la materia");
        if (excludedDates.findByMateriaIdAndFecha(subjectId, fecha).isEmpty()) {
            ExcludedDateEntity excluded = new ExcludedDateEntity(); excluded.setMateria(subject); excluded.setFecha(fecha); excludedDates.saveAndFlush(excluded);
        }
    }
    @Transactional
    public void include(Long subjectId, LocalDate fecha, String email) {
        subject(subjectId, email);
        excludedDates.delete(excludedDates.findByMateriaIdAndFecha(subjectId, fecha).orElseThrow(ApiException::notFound));
    }
    public List<LocalDate> excluded(Long subjectId, LocalDate desde, LocalDate hasta, String email) {
        subject(subjectId, email); checkRange(desde, hasta);
        return excludedDates.findByMateriaIdAndFechaBetweenOrderByFecha(subjectId, desde, hasta).stream().map(ExcludedDateEntity::getFecha).toList();
    }
    public AttendanceSummaryResponse summary(Long subjectId, LocalDate desde, LocalDate hasta, String email) {
        SubjectEntity subject = subject(subjectId, email); checkRange(desde, hasta);
        var notHeld = new HashSet<>(excludedDates.findByMateriaIdAndFechaBetweenOrderByFecha(subjectId, desde, hasta).stream().map(ExcludedDateEntity::getFecha).toList());
        var recorded = new HashMap<String, AttendanceControlEntity>();
        for (var item : controls.findByHorarioMateriaIdAndFechaBetween(subjectId, desde, hasta)) recorded.put(key(item.getHorario().getId(), item.getFecha(), item.getNumero()), item);
        List<AttendanceControlResponse> result = new ArrayList<>();
        LocalDate start = desde.isBefore(subject.getFechaInicio()) ? subject.getFechaInicio() : desde;
        LocalDate subjectEnd = subject.getFechaFin() == null
            ? LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
            : subject.getFechaFin();
        LocalDate end = hasta.isAfter(subjectEnd) ? subjectEnd : hasta;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (notHeld.contains(date)) continue;
            for (ScheduleEntity schedule : subject.getHorarios()) {
                if (Weekday.from(date.getDayOfWeek()) != schedule.getDiaSemana()) continue;
                for (int n = 1; n <= schedule.getControlesAsistencia(); n++) result.add(response(schedule, date, n, recorded.get(key(schedule.getId(), date, n))));
            }
        }
        int present = 0, absent = 0, justified = 0, pending = 0;
        for (var item : result) switch (item.estado()) {
            case "PRESENTE" -> present++;
            case "AUSENTE" -> absent++;
            case "JUSTIFICADA" -> justified++;
            default -> pending++;
        }
        int marked = present + absent + justified;
        BigDecimal percentage = marked == 0 ? null : BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(marked), 2, RoundingMode.HALF_UP);
        return new AttendanceSummaryResponse(present, absent, justified, pending, marked, percentage, result);
    }
    private static String key(Long id, LocalDate date, int number) { return id + ":" + date + ":" + number; }
    private void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365) throw new ApiException(400, "El rango debe ser válido y no superar 366 días");
    }
    private AttendanceControlResponse response(ScheduleEntity schedule, LocalDate date, int number, AttendanceControlEntity control) {
        long block = Duration.between(schedule.getHoraInicio(), schedule.getHoraFin()).toNanos() / schedule.getControlesAsistencia();
        LocalTime start = schedule.getHoraInicio().plusNanos(block * (number - 1));
        LocalTime end = number == schedule.getControlesAsistencia() ? schedule.getHoraFin() : start.plusNanos(block);
        return new AttendanceControlResponse(control == null ? null : control.getId(), schedule.getId(), date, number, start, end, control == null ? "PENDIENTE" : control.getEstado().name());
    }
}
