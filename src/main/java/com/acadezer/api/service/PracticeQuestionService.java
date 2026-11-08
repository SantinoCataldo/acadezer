package com.acadezer.api.service;

import com.acadezer.api.dto.response.PracticeQuestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PracticeQuestionService {
    private final NoteService apuntes;
    private final PracticeAiClient ai;

    public PracticeQuestionsResponse generate(Long id, String email) {
          String content = apuntes.owned(id, email).getContenido();
        return ai.generate(content);
    }
}
