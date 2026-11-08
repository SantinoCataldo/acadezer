package com.acadezer.api.controller;

import com.acadezer.api.dto.request.AttendanceRequest;
import com.acadezer.api.dto.response.AttendanceResponse;
import com.acadezer.api.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService service;

    @GetMapping("/api/materias/{materiaId}/asistencias")
    public List<AttendanceResponse> list(@PathVariable Long materiaId, Principal p) {
        return service.list(materiaId, p.getName());
    }

    @PostMapping("/api/materias/{materiaId}/asistencias")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AttendanceResponse create(@PathVariable Long materiaId, @Valid @RequestBody AttendanceRequest input, Principal p) {
        return service.create(materiaId, input, p.getName());
    }

    @GetMapping("/api/asistencias/{id}")
    public AttendanceResponse get(@PathVariable Long id, Principal p) {
        return service.get(id, p.getName());
    }

    @PutMapping("/api/asistencias/{id}")
    public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceRequest input, Principal p) {
        return service.update(id, input, p.getName());
    }

    @DeleteMapping("/api/asistencias/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal p) {
        service.delete(id, p.getName());
    }

}
