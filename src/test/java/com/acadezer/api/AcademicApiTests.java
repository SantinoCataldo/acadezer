package com.acadezer.api;

import com.acadezer.api.model.*;
import com.acadezer.api.enums.AttendanceStatus;
import com.acadezer.api.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademicApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired UserRepository users;
    @Autowired AttendanceRepository asistencias;
    @Autowired SubjectRepository materias;
    private String alice;
    private String bob;
    private long materia;

    @BeforeEach void accounts() throws Exception {
        alice = account(); bob = account();
        materia = call("POST", "/api/materias", "{\"nombre\":\"Análisis\"}", alice, 201).path("id").asLong();
    }

    private String account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        call("POST", "/auth/register", "{\"nombre\":\"Estudiante\",\"email\":\"" + email + "\",\"password\":\"password123\"}", null, 201);
        return call("POST", "/auth", "{\"email\":\"" + email + "\",\"password\":\"password123\"}", null, 200).path("accessToken").asText();
    }

    private JsonNode call(String method, String path, String body, String token, int status) throws Exception {
        var req = request(org.springframework.http.HttpMethod.valueOf(method), path);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType("application/json").content(body);
        String response = mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.createObjectNode() : json.readTree(response);
    }

    private String payload(String resource) {
        return switch (resource) {
            case "apuntes" -> "{\"titulo\":\"Derivadas\",\"contenido\":\"La derivada describe la tasa de cambio de una función respecto de su variable independiente. La derivada de una constante es cero; la de x al cuadrado es dos por x.\"}";
            case "pendientes" -> "{\"titulo\":\"Resolver guía\",\"fechaLimite\":\"2026-11-10\"}";
            case "evaluaciones" -> "{\"nombre\":\"Primer parcial\",\"fecha\":\"2026-11-10\",\"calificacion\":7.25}";
            default -> "{\"fecha\":\"2026-11-10\",\"estado\":\"PRESENTE\"}";
        };
    }

    @Test void publicHealthCheckDoesNotExposePrivateData() throws Exception {
        JsonNode health = call("GET", "/health", null, null, 200);
        assertThat(health.path("status").asText()).isEqualTo("UP");
        assertThat(health.size()).isEqualTo(1);
        call("GET", "/api/materias", null, null, 401);
        call("GET", "/api/admin/stats", null, null, 401);
    }

    @Test void matterIsolationAndValidation() throws Exception {
        String path = "/api/materias/" + materia;
        assertThat(call("GET", "/api/materias", null, bob, 200).size()).isZero();
        call("GET", path, null, bob, 404);
        call("PUT", path, "{\"nombre\":\"Robada\"}", bob, 404);
        call("DELETE", path, null, bob, 404);
        call("POST", "/api/materias", "{\"nombre\":\" \"}", alice, 400);
        call("POST", "/api/materias", "{\"nombre\":\"Otra\",\"usuarioId\":1}", alice, 400);
        call("PUT", path, "{\"nombre\":\"Álgebra\",\"descripcion\":\"Matrices\"}", alice, 200);
        assertThat(call("GET", path, null, alice, 200).path("nombre").asText()).isEqualTo("Álgebra");
        call("DELETE", path, null, alice, 204);
        call("GET", path, null, alice, 404);
    }

    @ParameterizedTest @CsvSource({"apuntes", "pendientes", "evaluaciones", "asistencias"})
    void childCrudAndIsolation(String resource) throws Exception {
        String nested = "/api/materias/" + materia + "/" + resource;
        String body = payload(resource);
        long id = call("POST", nested, body, alice, 201).path("id").asLong();
        String path = "/api/" + resource + "/" + id;
        call("GET", path, null, bob, 404);
        call("PUT", path, body, bob, 404);
        call("DELETE", path, null, bob, 404);
        call("GET", nested, null, bob, 404);
        call("POST", nested, body, bob, 404);
        assertThat(call("GET", nested, null, alice, 200).size()).isEqualTo(1);
        call("GET", path, null, alice, 200);
        call("PUT", path, body, alice, 200);
        call("DELETE", path, null, alice, 204);
        call("GET", path, null, alice, 404);
    }

    @Test void authenticationContracts() throws Exception {
        var error = call("GET", "/api/materias", null, null, 401);
        assertThat(error.has("timestamp")).isTrue();
        assertThat(error.path("path").asText()).isEqualTo("/api/materias");
        call("GET", "/api/materias", null, "broken.token", 401);
        call("POST", "/auth", "{\"email\":\"nobody@example.com\",\"password\":\"wrong\"}", null, 401);
        String email = UUID.randomUUID() + "@example.com";
        String body = "{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"password123\"}";
        JsonNode registered = call("POST", "/auth/register", body, null, 201);
        assertThat(registered.has("password")).isFalse();
        assertThat(users.findByEmail(email).orElseThrow().getPassword()).startsWith("$2");
        call("POST", "/auth/register", body.replace(email, email.toUpperCase()), null, 409);
        call("POST", "/auth", "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\"}", null, 200);
        call("POST", "/auth/register", body.replace("password123", "123"), null, 400);
        call("POST", "/auth/register", body.replace("password123", "á".repeat(40)), null, 400);
        call("POST", "/auth/register", body.replace("Ana", " "), null, 400);
        call("POST", "/auth/register", "{\"nombre\":\"Ana\",\"email\":\"invalid\",\"password\":\"password123\"}", null, 400);
    }

    @Test void angularCorsPreflightAndAuthenticatedResponse() throws Exception {
        mvc.perform(options("/api/materias")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(get("/api/materias")
                .header("Origin", "http://localhost:4200")
                .header("Authorization", "Bearer " + alice))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test void uniqueAttendanceOnCreateAndUpdateAndDatabase() throws Exception {
        String nested = "/api/materias/" + materia + "/asistencias";
        String body = payload("asistencias");
        long first = call("POST", nested, body, alice, 201).path("id").asLong();
        call("POST", nested, body, alice, 409);
        call("PUT", "/api/asistencias/" + first, body.replace("PRESENTE", "JUSTIFICADA"), alice, 200);
        long second = call("POST", nested, body.replace("2026-11-10", "2026-11-11"), alice, 201).path("id").asLong();
        call("PUT", "/api/asistencias/" + second, body, alice, 409);
        AttendanceEntity duplicate = new AttendanceEntity();
        duplicate.setMateria(materias.findById(materia).orElseThrow());
        duplicate.setFecha(LocalDate.of(2026, 11, 10)); duplicate.setEstado(AttendanceStatus.AUSENTE);
        assertThatThrownBy(() -> asistencias.saveAndFlush(duplicate)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        long other = call("POST", "/api/materias", "{\"nombre\":\"Física\"}", alice, 201).path("id").asLong();
        call("POST", "/api/materias/" + other + "/asistencias", body, alice, 201);
    }

    @Test void gradeBoundariesAndNullableGrade() throws Exception {
        String path = "/api/materias/" + materia + "/evaluaciones";
        String body = payload("evaluaciones");
        for (String grade : new String[]{"0", "10", "7.25", "null"}) call("POST", path, body.replace("7.25", grade), alice, 201);
        for (String grade : new String[]{"-0.01", "10.01", "7.123"}) call("POST", path, body.replace("7.25", grade), alice, 400);
        call("POST", path, "{\"nombre\":\"Parcial\"}", alice, 400);
    }

    @ParameterizedTest @CsvSource({"apuntes", "pendientes", "evaluaciones", "asistencias"})
    void rejectsMissingRequiredFields(String resource) throws Exception {
        call("POST", "/api/materias/" + materia + "/" + resource, "{}", alice, 400);
    }

    @Test void rejectsMalformedInputs() throws Exception {
        call("GET", "/api/materias/not-a-number", null, alice, 400);
        call("POST", "/api/materias", "{", alice, 400);
        call("POST", "/api/materias/" + materia + "/apuntes", "{\"titulo\":\" \",\"contenido\":\"\"}", alice, 400);
        call("POST", "/api/materias/" + materia + "/asistencias", "{\"fecha\":\"2026-11-10\",\"estado\":\"TARDE\"}", alice, 400);
        call("POST", "/api/materias/" + materia + "/asistencias", "{\"fecha\":\"bad-date\",\"estado\":\"PRESENTE\"}", alice, 400);
        call("GET", "/api/pendientes/proximos", null, alice, 400);
        mvc.perform(post("/api/materias").header("Authorization", "Bearer " + alice)
            .contentType("text/plain").content("texto"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
        mvc.perform(get("/api/materias").header("Authorization", "Bearer " + alice).accept("application/xml"))
            .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.status").value(406));
    }

    @Test void completionAndUpcomingRange() throws Exception {
        String nested = "/api/materias/" + materia + "/pendientes";
        long id = call("POST", nested, payload("pendientes"), alice, 201).path("id").asLong();
        String path = "/api/pendientes/" + id + "/completado";
        String upcoming = "/api/pendientes/proximos?desde=2026-11-09&hasta=2026-11-12";
        call("POST", nested, "{\"titulo\":\"Sin fecha\"}", alice, 201);
        call("POST", nested, payload("pendientes").replace("2026-11-10", "2026-11-08"), alice, 201);
        assertThat(call("GET", upcoming, null, alice, 200).size()).isEqualTo(1);
        assertThat(call("GET", upcoming, null, bob, 200).size()).isZero();
        call("PATCH", path, "{\"completado\":true}", bob, 404);
        call("PATCH", path, "{}", alice, 400);
        call("PATCH", path, "{\"completado\":true}", alice, 200);
        assertThat(call("GET", upcoming, null, alice, 200).size()).isZero();
        call("PATCH", path, "{\"completado\":false}", alice, 200);
        assertThat(call("GET", upcoming, null, alice, 200).size()).isEqualTo(1);
        call("GET", "/api/pendientes/proximos?desde=2026-11-12&hasta=2026-11-09", null, alice, 400);
    }

    @Test void deleteMatterCascadesAllChildrenButPreservesOtherMatter() throws Exception {
        long other = call("POST", "/api/materias", "{\"nombre\":\"Otra\"}", bob, 201).path("id").asLong();
        for (String resource : new String[]{"apuntes", "pendientes", "evaluaciones", "asistencias"}) {
            call("POST", "/api/materias/" + materia + "/" + resource, payload(resource), alice, 201);
            call("POST", "/api/materias/" + other + "/" + resource, payload(resource), bob, 201);
        }
        call("DELETE", "/api/materias/" + materia, null, alice, 204);
        assertThat(materias.existsById(materia)).isFalse();
        for (String resource : new String[]{"apuntes", "pendientes", "evaluaciones", "asistencias"})
            assertThat(call("GET", "/api/materias/" + other + "/" + resource, null, bob, 200).size()).isEqualTo(1);
    }

    @Test void aiOwnershipBeforeMissingConfiguration() throws Exception {
        long id = call("POST", "/api/materias/" + materia + "/apuntes", payload("apuntes"), alice, 201).path("id").asLong();
        String path = "/api/apuntes/" + id + "/preguntas";
        call("POST", path, null, bob, 404);
        call("POST", path, null, alice, 503);
        call("POST", "/api/apuntes/999999999/preguntas", null, alice, 404);
        call("GET", "/api/materias/" + materia + "/apuntes", null, alice, 200);
    }
}
