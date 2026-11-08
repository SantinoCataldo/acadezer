package com.acadezer.api.service;

import com.acadezer.api.exception.ApiException;
import com.acadezer.api.model.NoteEntity;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PracticeQuestionServiceTests {
    @Test void unauthorizedNoteCannotTriggerProvider() {
        NoteService notes = mock(NoteService.class);
        PracticeAiClient ai = mock(PracticeAiClient.class);
        when(notes.owned(1L, "other@example.com")).thenThrow(ApiException.notFound());
        assertThatThrownBy(() -> new PracticeQuestionService(notes, ai).generate(1L, "other@example.com")).isInstanceOf(ApiException.class);
        verifyNoInteractions(ai);
    }
    @Test void usesSavedContent() {
        NoteService notes = mock(NoteService.class);
        PracticeAiClient ai = mock(PracticeAiClient.class);
        NoteEntity note = new NoteEntity(); note.setContenido("Contenido persistido");
        when(notes.owned(1L, "owner@example.com")).thenReturn(note);
        new PracticeQuestionService(notes, ai).generate(1L, "owner@example.com");
        verify(ai).generate("Contenido persistido");
    }
}
