package com.acadezer.api.controller;

import com.acadezer.api.dto.response.PracticeQuestionsResponse;
import com.acadezer.api.service.PracticeQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class PracticeQuestionController {
    private final PracticeQuestionService preguntas;

    @PostMapping("/api/apuntes/{id}/preguntas")
    public PracticeQuestionsResponse generate(@PathVariable Long id, Principal principal) {
        return preguntas.generate(id, principal.getName());
    }
}
