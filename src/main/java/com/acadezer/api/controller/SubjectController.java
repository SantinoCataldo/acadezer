package com.acadezer.api.controller;

import com.acadezer.api.dto.request.SubjectRequest;
import com.acadezer.api.dto.response.SubjectResponse;
import com.acadezer.api.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService service;

    @GetMapping
    public List<SubjectResponse> list(Principal p) {
        return service.list(p.getName());
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@PathVariable Long id, Principal p) {
        return service.get(id, p.getName());
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody SubjectRequest input, Principal p) {
        return service.create(input, p.getName());
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest input, Principal p) {
        return service.update(id, input, p.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal p) {
        service.delete(id, p.getName());
    }
}
