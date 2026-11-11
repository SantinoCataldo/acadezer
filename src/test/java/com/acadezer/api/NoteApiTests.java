package com.acadezer.api;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class NoteApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    private String alice, bob;
    private long subject;

    private JsonNode call(String method, String path, String body, String token, int expected) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(body);
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.createObjectNode() : json.readTree(response);
    }

    private String account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        call("POST", "/auth/register", "{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"password123\"}", null, 201);
        return call("POST", "/auth", "{\"email\":\"" + email + "\",\"password\":\"password123\"}", null, 200).path("accessToken").asText();
    }

    @BeforeEach void setup() throws Exception {
        alice = account();
        bob = account();
        subject = call("POST", "/api/materias",
                "{\"nombre\":\"Programación I\",\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\",\"horarios\":[]}",
                alice, 201).path("id").asLong();
    }

    private String notes() { return "/api/materias/" + subject + "/apuntes"; }

    @Test void createsListsUpdatesAndDeletesNotesOfThatSubject() throws Exception {
        JsonNode created = call("POST", notes(), "{\"titulo\":\"Variables\",\"contenido\":\"<p>Tipos de datos</p>\"}", alice, 201);
        long note = created.path("id").asLong();
        assertThat(created.path("materiaId").asLong()).isEqualTo(subject);
        assertThat(created.path("creadoEn").asText()).isNotBlank();
        assertThat(created.path("actualizadoEn").asText()).isNotBlank();

        call("POST", notes(), "{\"titulo\":\"Estructuras\",\"contenido\":\"<p>if y while</p>\"}", alice, 201);
        JsonNode list = call("GET", notes(), null, alice, 200);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.path(0).path("titulo").asText()).isEqualTo("Variables");

        JsonNode updated = call("PUT", "/api/apuntes/" + note, "{\"titulo\":\"Variables y tipos\",\"contenido\":\"<p>Tipos</p>\"}", alice, 200);
        assertThat(updated.path("titulo").asText()).isEqualTo("Variables y tipos");
        assertThat(updated.path("actualizadoEn").asText()).isNotEqualTo(updated.path("creadoEn").asText());

        call("DELETE", "/api/apuntes/" + note, null, alice, 204);
        assertThat(call("GET", notes(), null, alice, 200).size()).isEqualTo(1);
        call("GET", "/api/apuntes/" + note, null, alice, 404);
    }

    @Test void notesOfAnotherUserAndUnknownSubjectAreNotFound() throws Exception {
        JsonNode created = call("POST", notes(), "{\"titulo\":\"Privado\",\"contenido\":\"solo Alice\"}", alice, 201);
        long note = created.path("id").asLong();

        call("GET", notes(), null, bob, 404);
        call("POST", notes(), "{\"titulo\":\"Ajeno\",\"contenido\":\"x\"}", bob, 404);
        call("GET", "/api/apuntes/" + note, null, bob, 404);
        call("PUT", "/api/apuntes/" + note, "{\"titulo\":\"Ajeno\",\"contenido\":\"x\"}", bob, 404);
        call("DELETE", "/api/apuntes/" + note, null, bob, 404);
        call("POST", "/api/materias/999999/apuntes", "{\"titulo\":\"x\",\"contenido\":\"x\"}", alice, 404);
        // La nota de Alice sigue intacta tras los intentos.
        assertThat(call("GET", "/api/apuntes/" + note, null, alice, 200).path("titulo").asText()).isEqualTo("Privado");
    }

    @Test void rejectsInvalidTitlesAndContent() throws Exception {
        call("POST", notes(), "{\"titulo\":\"\",\"contenido\":\"x\"}", alice, 400);
        call("POST", notes(), "{\"titulo\":\" \",\"contenido\":\"x\"}", alice, 400);
        call("POST", notes(), "{\"titulo\":\"x\",\"contenido\":\"\"}", alice, 400);
        call("POST", notes(), "{\"titulo\":\"" + "a".repeat(201) + "\",\"contenido\":\"x\"}", alice, 400);
        call("POST", notes(), "{\"titulo\":\"x\",\"contenido\":\"" + "a".repeat(20001) + "\"}", alice, 400);
        assertThat(call("GET", notes(), null, alice, 200).size()).isZero();
    }

    @Test void deletingTheSubjectRemovesItsNotes() throws Exception {
        call("POST", notes(), "{\"titulo\":\"Se borra con la materia\",\"contenido\":\"x\"}", alice, 201);
        call("DELETE", "/api/materias/" + subject, null, alice, 204);
        long other = call("POST", "/api/materias",
                "{\"nombre\":\"Otra\",\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\",\"horarios\":[]}",
                alice, 201).path("id").asLong();
        call("GET", "/api/materias/" + other + "/apuntes", null, alice, 200);
        assertThat(call("GET", "/api/materias/" + other + "/apuntes", null, alice, 200).size()).isZero();
    }
}
