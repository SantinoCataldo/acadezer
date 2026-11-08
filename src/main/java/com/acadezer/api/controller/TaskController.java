package com.acadezer.api.controller;

import com.acadezer.api.dto.request.TaskRequest;
import com.acadezer.api.dto.request.TaskCompletionRequest;
import com.acadezer.api.dto.response.TaskResponse;
import com.acadezer.api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;

    @GetMapping("/api/materias/{materiaId}/pendientes")
    public List<TaskResponse> list(@PathVariable Long materiaId, Principal p) {
        return service.list(materiaId, p.getName());
    }

    @PostMapping("/api/materias/{materiaId}/pendientes")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long materiaId, @Valid @RequestBody TaskRequest input, Principal p) {
        return service.create(materiaId, input, p.getName());
    }

    @GetMapping("/api/pendientes/{id}")
    public TaskResponse get(@PathVariable Long id, Principal p) {
        return service.get(id, p.getName());
    }

    @PutMapping("/api/pendientes/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest input, Principal p) {
        return service.update(id, input, p.getName());
    }

    @DeleteMapping("/api/pendientes/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal p) {
        service.delete(id, p.getName());
    }

@PatchMapping("/api/pendientes/{id}/completado")
    public TaskResponse complete(@PathVariable Long id, @Valid @RequestBody TaskCompletionRequest input, Principal p) {
        return service.complete(id, input.completado(), p.getName());
    }

    @GetMapping("/api/pendientes/proximos")
    public List<TaskResponse> upcoming(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
            Principal p) {
        return service.upcoming(desde, hasta, p.getName());
    }

}
