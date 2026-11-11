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
class ScheduleAttendanceApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    private String alice, bob;
    private long subject, schedule;
    private static final String SUBJECT = "{\"nombre\":\"Matemática\",\"fechaInicio\":\"2026-11-01\",\"fechaFin\":\"2026-11-30\",\"horarios\":[{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\",\"controlesAsistencia\":2}]}";

    private JsonNode call(String method, String path, String body, String token, int expected) throws Exception {
        var request = request(HttpMethod.valueOf(method), path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(body);
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.createObjectNode() : json.readTree(response);
    }
    private String account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        call("POST", "/auth/register", "{\"nombre\":\"Ana\",\"email\":\""+email+"\",\"password\":\"password123\"}", null, 201);
        return call("POST", "/auth", "{\"email\":\""+email+"\",\"password\":\"password123\"}", null, 200).path("accessToken").asText();
    }
    @BeforeEach void setup() throws Exception {
        alice = account(); bob = account();
        JsonNode created = call("POST", "/api/materias", SUBJECT, alice, 201);
        subject = created.path("id").asLong(); schedule = created.path("horarios").path(0).path("id").asLong();
        assertThat(created.path("porcentajeAsistenciaMinima").decimalValue()).isEqualByComparingTo("75");
        assertThat(created.has("descripcion")).isFalse(); assertThat(created.has("periodo")).isFalse();
    }
    private String controls() { return "/api/materias/" + subject + "/controles-asistencia?desde=2026-11-09&hasta=2026-11-09"; }
    private String control(int n) { return "/api/materias/" + subject + "/horarios/" + schedule + "/controles/2026-11-09/" + n; }
    private JsonNode mark(int n, String state) throws Exception { return call("PUT", control(n), "{\"estado\":\""+state+"\"}", alice, 200); }

    @Test void twoBlocksCountSeparatelyAndPendingAreNotMarkedAbsent() throws Exception {
        JsonNode initial = call("GET", controls(), null, alice, 200);
        assertThat(initial.path("pendientes").asInt()).isEqualTo(2);
        assertThat(initial.path("porcentajeAsistencia").isNull()).isTrue();
        assertThat(initial.path("controles").path(0).path("horaInicio").asText()).startsWith("08:00");
        assertThat(initial.path("controles").path(0).path("horaFin").asText()).startsWith("10:00");
        assertThat(initial.path("controles").path(1).path("horaInicio").asText()).startsWith("10:00");
        mark(1, "PRESENTE");
        JsonNode oneMarked = call("GET", controls(), null, alice, 200);
        assertThat(oneMarked.path("pendientes").asInt()).isEqualTo(1);
        assertThat(oneMarked.path("porcentajeAsistencia").decimalValue()).isEqualByComparingTo("100");
        mark(2, "AUSENTE");
        JsonNode half = call("GET", controls(), null, alice, 200);
        assertThat(half.path("porcentajeAsistencia").decimalValue()).isEqualByComparingTo("50");
        assertThat(half.path("cumpleMinimo").asBoolean()).isFalse();
        mark(2, "JUSTIFICADA");
        JsonNode justified = call("GET", controls(), null, alice, 200);
        assertThat(justified.path("justificadas").asInt()).isEqualTo(1);
        assertThat(justified.path("porcentajeAsistencia").decimalValue()).isEqualByComparingTo("50");
        mark(1, "AUSENTE"); mark(2, "AUSENTE");
        JsonNode absent = call("GET", controls(), null, alice, 200);
        assertThat(absent.path("ausentes").asInt()).isEqualTo(2);
        assertThat(absent.path("porcentajeAsistencia").decimalValue()).isEqualByComparingTo("0");
        call("DELETE", control(1), null, alice, 204);
        assertThat(call("GET", controls(), null, alice, 200).path("pendientes").asInt()).isEqualTo(1);
    }
    @Test void exclusionRemovesClassFromCalculationWithoutDeletingMarks() throws Exception {
        mark(1, "PRESENTE"); mark(2, "AUSENTE");
        String path = "/api/materias/"+subject+"/fechas-sin-cursada/2026-11-09";
        call("PUT", path, null, alice, 204);
        JsonNode summary = call("GET", controls(), null, alice, 200);
        assertThat(summary.path("controles").size()).isZero();
        assertThat(summary.path("porcentajeAsistencia").isNull()).isTrue();
        call("PUT", control(1), "{\"estado\":\"PRESENTE\"}", alice, 409);
        call("DELETE", path, null, alice, 204);
        assertThat(call("GET", controls(), null, alice, 200).path("porcentajeAsistencia").decimalValue()).isEqualByComparingTo("50");
    }
    @Test void ownerChecksApplyToSchedulesControlsAndExceptions() throws Exception {
        call("GET", controls(), null, bob, 404);
        call("PUT", control(1), "{\"estado\":\"PRESENTE\"}", bob, 404);
        call("PUT", "/api/materias/"+subject+"/fechas-sin-cursada/2026-11-09", null, bob, 404);
        long other = call("POST", "/api/materias", SUBJECT, bob, 201).path("id").asLong();
        call("PUT", "/api/materias/"+other+"/horarios/"+schedule+"/controles/2026-11-09/1", "{\"estado\":\"PRESENTE\"}", bob, 404);
    }
    @Test void validatesDatesHoursPercentageAndControls() throws Exception {
        call("POST", "/api/materias", SUBJECT.replace("2026-11-30", "2026-10-01"), alice, 400);
        call("POST", "/api/materias", SUBJECT.replace("\"horarios\":", "\"porcentajeAsistenciaMinima\":101,\"horarios\":"), alice, 400);
        call("POST", "/api/materias", SUBJECT.replace("12:00", "07:00"), alice, 400);
        call("POST", "/api/materias", SUBJECT.replace("\"controlesAsistencia\":2", "\"controlesAsistencia\":3"), alice, 400);
        call("POST", "/api/materias", SUBJECT.replace("\"horarios\":", "\"periodo\":\"ANUAL\",\"horarios\":"), alice, 400);
        call("PUT", control(3), "{\"estado\":\"PRESENTE\"}", alice, 400);
        call("PUT", control(1).replace("2026-11-09", "2026-11-10"), "{\"estado\":\"PRESENTE\"}", alice, 400);
        call("GET", controls().replace("2026-11-09&hasta=2026-11-09", "2026-11-01&hasta=2028-11-01"), null, alice, 400);
    }
    @Test void recordedControlsProtectSchedulesAndCascadeOnSubjectDeletion() throws Exception {
        mark(1, "PRESENTE");
        String retained = SUBJECT.replace("\"diaSemana\":", "\"id\":" + schedule + ",\"diaSemana\":");
        JsonNode updated = call("PUT", "/api/materias/"+subject, retained.replace("2026-11-30", "2026-11-20"), alice, 200);
        assertThat(updated.path("horarios").path(0).path("id").asLong()).isEqualTo(schedule);
        call("PUT", "/api/materias/"+subject, retained.replace("2026-11-30", "2026-11-08"), alice, 409);
        call("PUT", "/api/materias/"+subject, SUBJECT, alice, 409);
        call("DELETE", "/api/materias/"+subject, null, alice, 204);
        call("GET", controls(), null, alice, 404);
    }
}
