package com.acadezer.api;

import com.acadezer.api.enums.UserRole;
import com.acadezer.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired UserRepository users;
    private Account admin;
    private Account student;
    private record Account(long id, String email, String token) {}

    private JsonNode call(String method, String path, String body, String token, int expected) throws Exception {
        var req = request(HttpMethod.valueOf(method), path);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType("application/json").content(body);
        String response = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.createObjectNode() : json.readTree(response);
    }

    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        JsonNode registered = call("POST", "/auth/register",
            "{\"nombre\":\"Estudiante\",\"email\":\"" + email + "\",\"password\":\"password123\"}", null, 201);
        assertThat(registered.path("role").asText()).isEqualTo("STUDENT");
        assertThat(registered.path("enabled").asBoolean()).isTrue();
        assertThat(registered.has("password")).isFalse();
        String token = call("POST", "/auth", login(email), null, 200).path("accessToken").asText();
        return new Account(registered.path("id").asLong(), email, token);
    }

    private String login(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
    }

    @BeforeEach void setup() throws Exception {
        admin = account();
        var entity = users.findById(admin.id()).orElseThrow();
        entity.setRole(UserRole.ADMIN);
        users.saveAndFlush(entity);
        student = account();
    }

    @Test void studentsCannotAccessAnyAdminOperation() throws Exception {
        call("GET", "/api/admin/users", null, null, 401);
        var denied = call("GET", "/api/admin/users", null, student.token(), 403);
        assertThat(denied.path("status").asInt()).isEqualTo(403);
        call("GET", "/api/admin/stats", null, student.token(), 403);
        call("PATCH", "/api/admin/users/" + admin.id() + "/enabled", "{\"enabled\":false}", student.token(), 403);
    }

    @Test void clientCannotAssignRoleOrAccountState() throws Exception {
        String body = "{\"nombre\":\"Intento\",\"email\":\"" + UUID.randomUUID() + "@example.com\",\"password\":\"password123\"";
        call("POST", "/auth/register", body + ",\"role\":\"ADMIN\"}", null, 400);
        call("POST", "/auth/register", body + ",\"enabled\":false}", null, 400);
    }

    @Test void administratorListsOnlySafeUserData() throws Exception {
        JsonNode response = call("GET", "/api/admin/users", null, admin.token(), 200);
        assertThat(response.isArray()).isTrue();
        assertThat(response.size()).isGreaterThanOrEqualTo(2);
        boolean foundStudent = false;
        for (JsonNode user : response) {
            assertThat(user.has("password")).isFalse();
            assertThat(user.has("accessToken")).isFalse();
            assertThat(user.has("nombre")).isTrue();
            assertThat(user.has("role")).isTrue();
            if (user.path("id").asLong() == student.id()) foundStudent = true;
        }
        assertThat(foundStudent).isTrue();
    }

    @Test void disabledStudentCannotLoginOrUseExistingToken() throws Exception {
        String path = "/api/admin/users/" + student.id() + "/enabled";
        assertThat(call("PATCH", path, "{\"enabled\":false}", admin.token(), 200).path("enabled").asBoolean()).isFalse();
        call("POST", "/auth", login(student.email()), null, 401);
        call("GET", "/api/materias", null, student.token(), 401);
        call("GET", "/auth/me", null, student.token(), 401);
        call("PATCH", path, "{\"enabled\":true}", admin.token(), 200);
        call("POST", "/auth", login(student.email()), null, 200);
        call("GET", "/api/materias", null, student.token(), 200);
    }

    @Test void administratorsCannotDisableAdminAccountsOrSendInvalidState() throws Exception {
        call("PATCH", "/api/admin/users/" + admin.id() + "/enabled", "{\"enabled\":false}", admin.token(), 409);
        call("PATCH", "/api/admin/users/999999999/enabled", "{\"enabled\":false}", admin.token(), 404);
        call("PATCH", "/api/admin/users/" + student.id() + "/enabled", "{}", admin.token(), 400);
        call("PATCH", "/api/admin/users/" + student.id() + "/enabled", "{\"enabled\":null}", admin.token(), 400);
    }

    @Test void adminRoleDoesNotBypassAcademicPrivacy() throws Exception {
        long subject = call("POST", "/api/materias", "{\"nombre\":\"Privada\",\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\",\"horarios\":[]}", student.token(), 201).path("id").asLong();
        long note = call("POST", "/api/materias/" + subject + "/apuntes",
            "{\"titulo\":\"Privado\",\"contenido\":\"Texto personal\"}", student.token(), 201).path("id").asLong();
        call("GET", "/api/materias/" + subject, null, admin.token(), 404);
        call("DELETE", "/api/materias/" + subject, null, admin.token(), 404);
        call("GET", "/api/apuntes/" + note, null, admin.token(), 404);
        call("POST", "/api/apuntes/" + note + "/preguntas", null, admin.token(), 404);
        call("GET", "/api/materias/" + subject + "/apuntes", null, admin.token(), 404);
    }

    @Test void frontendGetsCurrentRoleAndDatabaseRoleChangesApplyImmediately() throws Exception {
        assertThat(call("GET", "/auth/me", null, admin.token(), 200).path("role").asText()).isEqualTo("ADMIN");
        assertThat(call("POST", "/auth", login(admin.email()), null, 200).path("usuario").path("role").asText()).isEqualTo("ADMIN");
        var user = users.findById(admin.id()).orElseThrow();
        user.setRole(UserRole.STUDENT); users.saveAndFlush(user);
        call("GET", "/api/admin/stats", null, admin.token(), 403);
        assertThat(call("GET", "/auth/me", null, admin.token(), 200).path("role").asText()).isEqualTo("STUDENT");
    }

    @Test void adminStatsContainCountsWithoutPrivateContent() throws Exception {
        JsonNode stats = call("GET", "/api/admin/stats", null, admin.token(), 200);
        assertThat(stats.path("usuarios").asLong()).isEqualTo(users.count());
        assertThat(stats.path("usuariosHabilitados").asLong()).isEqualTo(users.countByEnabledTrue());
        assertThat(stats.path("estudiantes").asLong()).isEqualTo(users.countByRole(UserRole.STUDENT));
        assertThat(stats.path("administradores").asLong()).isEqualTo(users.countByRole(UserRole.ADMIN));
        assertThat(stats.has("contenido")).isFalse();
        assertThat(stats.has("materias")).isTrue();
        assertThat(stats.has("apuntes")).isTrue();
        assertThat(stats.has("pendientes")).isTrue();
        assertThat(stats.has("evaluaciones")).isTrue();
        assertThat(stats.has("asistencias")).isTrue();
    }
}
