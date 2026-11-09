package com.acadezer.api.service;

import com.acadezer.api.exception.ApiException;
import com.acadezer.api.model.NoteEntity;
import com.acadezer.api.ratelimit.AiRequestLimiter;
import com.acadezer.api.exception.RateLimitExceededException;
import com.acadezer.api.dto.response.PracticeQuestionsResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PracticeQuestionServiceTests {
    @Test void unauthorizedNoteCannotTriggerProvider() {
        NoteService notes = mock(NoteService.class);
        PracticeAiClient ai = mock(PracticeAiClient.class);
        AiRequestLimiter limits = mock(AiRequestLimiter.class);
        when(notes.owned(1L, "other@example.com")).thenThrow(ApiException.notFound());
        assertThatThrownBy(() -> new PracticeQuestionService(notes, ai, limits).generate(1L, "other@example.com")).isInstanceOf(ApiException.class);
        verifyNoInteractions(ai, limits);
    }
    @Test void usesSavedContent() {
        NoteService notes = mock(NoteService.class);
        PracticeAiClient ai = mock(PracticeAiClient.class);
        NoteEntity note = new NoteEntity(); note.setContenido("Contenido persistido");
        when(notes.owned(1L, "owner@example.com")).thenReturn(note);
        new PracticeQuestionService(notes, ai, new AiRequestLimiter(2, Duration.ofHours(1), 2, 1, 100)).generate(1L, "owner@example.com");
        verify(ai).generate("Contenido persistido");
    }

    @Test void missingAuthenticationDoesNotReadNotesOrConsumeQuota() {
        var notes = mock(NoteService.class);
        var ai = mock(PracticeAiClient.class);
        var limits = mock(AiRequestLimiter.class);
        assertThatThrownBy(() -> new PracticeQuestionService(notes, ai, limits).generate(1L, null))
            .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        verifyNoInteractions(notes, ai, limits);
    }

    @Test void quotaAppliesAcrossNotesAndRejectsBeforeProviderCall() {
        var notes = mock(NoteService.class);
        var ai = mock(PracticeAiClient.class);
        var note = new NoteEntity(); note.setContenido("Texto guardado");
        when(notes.owned(anyLong(), eq("owner@example.com"))).thenReturn(note);
        var service = new PracticeQuestionService(notes, ai, new AiRequestLimiter(1, Duration.ofHours(1), 1, 1, 100));
        service.generate(1L, "owner@example.com");
        assertThatThrownBy(() -> service.generate(2L, "owner@example.com")).isInstanceOf(RateLimitExceededException.class);
        verify(ai, times(1)).generate(anyString());
        verify(notes).owned(2L, "owner@example.com");
    }

    @ParameterizedTest
    @ValueSource(ints = {502, 504})
    void providerErrorsAndTimeoutsReleaseConcurrency(int status) {
        var notes = mock(NoteService.class);
        var ai = mock(PracticeAiClient.class);
        var note = new NoteEntity(); note.setContenido("Texto guardado");
        when(notes.owned(anyLong(), anyString())).thenReturn(note);
        when(ai.generate(anyString())).thenThrow(new ApiException(status, "Fallo de prueba"))
            .thenReturn(new PracticeQuestionsResponse(List.of()));
        var service = new PracticeQuestionService(notes, ai, new AiRequestLimiter(2, Duration.ofHours(1), 1, 1, 100));
        assertThatThrownBy(() -> service.generate(1L, "owner@example.com")).isInstanceOf(ApiException.class);
        assertThatCode(() -> service.generate(1L, "owner@example.com")).doesNotThrowAnyException();
        verify(ai, times(2)).generate(anyString());
    }

    @Test void failedBillableAttemptsRemainChargedToQuota() {
        var notes = mock(NoteService.class);
        var ai = mock(PracticeAiClient.class);
        var note = new NoteEntity(); note.setContenido("Texto guardado");
        when(notes.owned(anyLong(), anyString())).thenReturn(note);
        when(ai.generate(anyString())).thenThrow(new ApiException(504, "Timeout de prueba"));
        var service = new PracticeQuestionService(notes, ai, new AiRequestLimiter(1, Duration.ofHours(1), 1, 1, 100));
        assertThatThrownBy(() -> service.generate(1L, "owner@example.com")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.generate(1L, "owner@example.com")).isInstanceOf(RateLimitExceededException.class);
        verify(ai, times(1)).generate(anyString());
    }

    @Test void invalidOrUnconfiguredInputDoesNotConsumeQuota() {
        var notes = mock(NoteService.class);
        var ai = mock(PracticeAiClient.class);
        var limits = mock(AiRequestLimiter.class);
        var note = new NoteEntity(); note.setContenido("Texto insuficiente");
        when(notes.owned(1L, "owner@example.com")).thenReturn(note);
        doThrow(new ApiException(422, "Contenido insuficiente")).when(ai).validateInput(anyString());
        assertThatThrownBy(() -> new PracticeQuestionService(notes, ai, limits).generate(1L, "owner@example.com")).isInstanceOf(ApiException.class);
        verifyNoInteractions(limits);
        verify(ai, never()).generate(anyString());
    }
}
