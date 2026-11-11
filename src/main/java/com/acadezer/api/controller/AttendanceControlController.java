package com.acadezer.api.controller;

import com.acadezer.api.dto.request.AttendanceControlRequest;
import com.acadezer.api.dto.response.*;
import com.acadezer.api.service.AttendanceControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/materias/{materiaId}") @RequiredArgsConstructor
public class AttendanceControlController {
    private final AttendanceControlService service;
    @GetMapping("/controles-asistencia")
    public AttendanceSummaryResponse summary(@PathVariable Long materiaId, @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Principal p) {
        return service.summary(materiaId, desde, hasta, p.getName());
    }
    @PutMapping("/horarios/{horarioId}/controles/{fecha}/{numero}")
    public AttendanceControlResponse put(@PathVariable Long materiaId, @PathVariable Long horarioId, @PathVariable LocalDate fecha,
                                         @PathVariable int numero, @Valid @RequestBody AttendanceControlRequest request, Principal p) {
        return service.put(materiaId, horarioId, fecha, numero, request, p.getName());
    }
    @DeleteMapping("/horarios/{horarioId}/controles/{fecha}/{numero}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long materiaId, @PathVariable Long horarioId, @PathVariable LocalDate fecha, @PathVariable int numero, Principal p) {
        service.delete(materiaId, horarioId, fecha, numero, p.getName());
    }
    @GetMapping("/fechas-sin-cursada")
    public List<LocalDate> excluded(@PathVariable Long materiaId, @RequestParam LocalDate desde, @RequestParam LocalDate hasta, Principal p) {
        return service.excluded(materiaId, desde, hasta, p.getName());
    }
    @PutMapping("/fechas-sin-cursada/{fecha}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exclude(@PathVariable Long materiaId, @PathVariable LocalDate fecha, Principal p) { service.exclude(materiaId, fecha, p.getName()); }
    @DeleteMapping("/fechas-sin-cursada/{fecha}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void include(@PathVariable Long materiaId, @PathVariable LocalDate fecha, Principal p) { service.include(materiaId, fecha, p.getName()); }
}
