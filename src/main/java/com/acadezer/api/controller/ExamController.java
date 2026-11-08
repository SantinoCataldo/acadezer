package com.acadezer.api.controller;

import com.acadezer.api.dto.request.ExamRequest;
import com.acadezer.api.dto.response.ExamResponse;
import com.acadezer.api.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExamController {
    private final ExamService service;

    @GetMapping("/api/materias/{materiaId}/evaluaciones")
    public List<ExamResponse> list(@PathVariable Long materiaId, Principal p) {
        return service.list(materiaId, p.getName());
    }

    @PostMapping("/api/materias/{materiaId}/evaluaciones")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ExamResponse create(@PathVariable Long materiaId, @Valid @RequestBody ExamRequest input, Principal p) {
        return service.create(materiaId, input, p.getName());
    }

    @GetMapping("/api/evaluaciones/{id}")
    public ExamResponse get(@PathVariable Long id, Principal p) {
        return service.get(id, p.getName());
    }

    @PutMapping("/api/evaluaciones/{id}")
    public ExamResponse update(@PathVariable Long id, @Valid @RequestBody ExamRequest input, Principal p) {
        return service.update(id, input, p.getName());
    }

    @DeleteMapping("/api/evaluaciones/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal p) {
        service.delete(id, p.getName());
    }

}
