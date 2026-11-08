package com.acadezer.api.dto.response;

import java.util.List;

public record PracticeQuestionsResponse(
        List<PracticeQuestionResponse> preguntas) {
}
