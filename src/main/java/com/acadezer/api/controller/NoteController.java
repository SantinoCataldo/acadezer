package com.acadezer.api.controller;

import com.acadezer.api.dto.request.NoteRequest;
import com.acadezer.api.dto.response.NoteResponse;
import com.acadezer.api.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoteController {
    private final NoteService service;

    @GetMapping("/api/materias/{materiaId}/apuntes")
    public List<NoteResponse> list(@PathVariable Long materiaId, Principal p) {
        return service.list(materiaId, p.getName());
    }

    @PostMapping("/api/materias/{materiaId}/apuntes")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public NoteResponse create(@PathVariable Long materiaId, @Valid @RequestBody NoteRequest input, Principal p) {
        return service.create(materiaId, input, p.getName());
    }

    @GetMapping("/api/apuntes/{id}")
    public NoteResponse get(@PathVariable Long id, Principal p) {
        return service.get(id, p.getName());
    }

    @PutMapping("/api/apuntes/{id}")
    public NoteResponse update(@PathVariable Long id, @Valid @RequestBody NoteRequest input, Principal p) {
        return service.update(id, input, p.getName());
    }

    @DeleteMapping("/api/apuntes/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal p) {
        service.delete(id, p.getName());
    }

}
