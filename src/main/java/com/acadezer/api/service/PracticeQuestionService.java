package com.acadezer.api.service;

import com.acadezer.api.dto.response.PracticeQuestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import com.acadezer.api.ratelimit.AiRequestLimiter;

@Service
@RequiredArgsConstructor
public class PracticeQuestionService {
    private final NoteService apuntes;
    private final PracticeAiClient ai;
    private final AiRequestLimiter limits;

    public PracticeQuestionsResponse generate(Long id, String email) {
        if (email == null || email.isBlank()) throw new BadCredentialsException("Autenticación requerida");
        String content = apuntes.owned(id, email).getContenido();
        ai.validateInput(content);
        try (var permit = limits.acquire(email)) {
            return ai.generate(content);
        }
    }
}
