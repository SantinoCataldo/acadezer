package com.acadezer.api;

import com.acadezer.api.dto.response.PracticeQuestionsResponse;
import com.acadezer.api.exception.ApiException;
import com.acadezer.api.repository.UserRepository;
import com.acadezer.api.service.PracticeAiClient;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.limits.login.account=2", "app.limits.login.ip=3",
    "app.limits.register.account=2", "app.limits.register.ip=3",
    "app.limits.ai.requests=2", "app.limits.ai.max-concurrent=1",
    "app.limits.ai.max-concurrent-per-user=1", "app.limits.trusted-proxy-cidrs="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestLimitsApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired UserRepository users;
    @MockitoBean PracticeAiClient ai;
    private static final AtomicInteger NEXT_IP = new AtomicInteger();
    private record Account(String email, String token, long note) {}

    @BeforeEach void configureProvider() {
        when(ai.generate(anyString())).thenReturn(new PracticeQuestionsResponse(List.of()));
    }

    private String ip() {
        return "198.51.100." + NEXT_IP.incrementAndGet();
    }

    private MvcResult call(String method, String path, String body, String token, String peer, String forwarded, int expected) throws Exception {
        var req = request(HttpMethod.valueOf(method), path).with(r -> { r.setRemoteAddr(peer); return r; });
        req.header("Origin", "http://localhost:4200");
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType("application/json").content(body);
        if (forwarded != null) req.header("X-Forwarded-For", forwarded);
        return mvc.perform(req).andExpect(status().is(expected)).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void limited(MvcResult result, String path) throws Exception {
        assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After"))).isPositive();
        assertThat(result.getResponse().getHeader("Access-Control-Expose-Headers")).contains("Retry-After");
        JsonNode error = body(result);
        assertThat(error.path("status").asInt()).isEqualTo(429);
        assertThat(error.path("path").asText()).isEqualTo(path);
        assertThat(error.has("timestamp")).isTrue();
    }

    private String login(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
    }

    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        call("POST", "/auth/register", "{\"nombre\":\"Estudiante\",\"email\":\"" + email + "\",\"password\":\"password123\"}", null, ip(), null, 201);
        String token = body(call("POST", "/auth", login(email), null, ip(), null, 200)).path("accessToken").asText();
        long subject = body(call("POST", "/api/materias", "{\"nombre\":\"Materia\",\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\",\"horarios\":[]}", token, ip(), null, 201)).path("id").asLong();
        long note = body(call("POST", "/api/materias/" + subject + "/apuntes",
            "{\"titulo\":\"Apunte\",\"contenido\":\"Contenido de prueba sin proveedor real\"}", token, ip(), null, 201)).path("id").asLong();
        return new Account(email, token, note);
    }

    @Test void loginAccountLimitIsNormalizedAcrossIpsAndNeverDisablesTheAccount() throws Exception {
        Account account = account(); // First login already consumes one attempt.
        call("POST", "/auth", login(account.email()).replace("password123", "wrong"), null, ip(), null, 401);
        limited(call("POST", "/auth", login(account.email().toUpperCase()), null, ip(), null, 429), "/auth");
        assertThat(users.findByEmail(account.email()).orElseThrow().isEnabled()).isTrue();
        call("GET", "/auth/me", null, account.token(), ip(), null, 200);
    }

    @Test void loginIpLimitIncludesMalformedBodiesAndCannotBeEvadedWithForwardingHeaders() throws Exception {
        String peer = ip();
        for (int i = 0; i < 3; i++) call("POST", "/auth", "{", null, peer, "203.0.113." + i, 400);
        limited(call("POST", "/auth", login("another@example.com"), null, peer, "1.1.1.1", 429), "/auth");
        call("POST", "/auth", login(UUID.randomUUID() + "@example.com"), null, ip(), null, 401);
    }

    @Test void registerHasIndependentAccountAndIpLimits() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String registration = "{\"nombre\":\"Estudiante\",\"email\":\"" + email + "\",\"password\":\"password123\"}";
        call("POST", "/auth/register", registration, null, ip(), null, 201);
        call("POST", "/auth/register", registration.replace(email, email.toUpperCase()), null, ip(), null, 409);
        limited(call("POST", "/auth/register", registration, null, ip(), null, 429), "/auth/register");
        String peer = ip();
        for (int i = 0; i < 3; i++) call("POST", "/auth/register", "{}", null, peer, null, 400);
        limited(call("POST", "/auth/register", registration, null, peer, null, 429), "/auth/register");
        // Registration throttling does not consume the login window for the same IP/account.
        call("POST", "/auth", login(email), null, peer, null, 200);
    }

    @Test void aiQuotaRespectsOwnershipFirstAndNeverCallsProviderOn429() throws Exception {
        Account alice = account();
        Account bob = account();
        String own = "/api/apuntes/" + alice.note() + "/preguntas";
        call("POST", own, null, alice.token(), ip(), null, 200);
        call("POST", own, null, alice.token(), ip(), null, 200);
        call("POST", "/api/apuntes/" + bob.note() + "/preguntas", null, alice.token(), ip(), null, 404);
        call("POST", own, null, null, ip(), null, 401);
        limited(call("POST", own, null, alice.token(), ip(), null, 429), own);
        verify(ai, times(2)).generate(anyString());
        call("POST", "/api/apuntes/" + bob.note() + "/preguntas", null, bob.token(), ip(), null, 200);
        verify(ai, times(3)).generate(anyString());
    }

    @Test void concurrentRequestsAreRejectedAndTimeoutReleasesSlot() throws Exception {
        Account alice = account();
        Account bob = account();
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(ai.generate(anyString())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                providerStarted.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Provider test was not released");
                throw new ApiException(504, "Timeout de prueba");
            }
            return new PracticeQuestionsResponse(List.of());
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?> first = executor.submit(() -> call("POST", "/api/apuntes/" + alice.note() + "/preguntas", null, alice.token(), ip(), null, 504));
            try {
                assertThat(providerStarted.await(5, TimeUnit.SECONDS)).isTrue();
                String bobPath = "/api/apuntes/" + bob.note() + "/preguntas";
                limited(call("POST", bobPath, null, bob.token(), ip(), null, 429), bobPath);
                verify(ai, times(1)).generate(anyString());
            } finally { release.countDown(); }
            first.get(5, TimeUnit.SECONDS);
        }
        // Both of Bob's requests remain available after the concurrency denial.
        for (int i = 0; i < 2; i++) call("POST", "/api/apuntes/" + bob.note() + "/preguntas", null, bob.token(), ip(), null, 200);
        verify(ai, times(3)).generate(anyString());
    }
}
