package com.acadezer.api;

import com.acadezer.api.repository.RefreshTokenRepository;
import com.acadezer.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RefreshTokenApiTests {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;

    private JsonNode post(String path, String body, int expected) throws Exception {
        String response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contentType("application/json").content(body))
            .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.createObjectNode() : json.readTree(response);
    }
    private String request(String token) { return "{\"refreshToken\":\"" + token + "\"}"; }
    private JsonNode login(String email) throws Exception {
        return post("/auth", "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200);
    }
    private String register() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        post("/auth/register", "{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"password123\"}", 201);
        return email;
    }

    @Test void refreshRotatesAndLogoutRevokes() throws Exception {
        String email = register();
        JsonNode first = login(email);
        String refresh = first.path("refreshToken").asText();
        assertThat(refresh).isNotBlank();
        assertThat(tokens.findAll()).noneMatch(token -> token.getTokenHash().equals(refresh));
        JsonNode next = post("/auth/refresh", request(refresh), 200);
        assertThat(next.path("refreshToken").asText()).isNotEqualTo(refresh);
        assertThat(next.path("usuario").path("email").asText()).isEqualTo(email);
        post("/auth/refresh", request(refresh), 401);
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + next.path("accessToken").asText())).andExpect(status().isOk());
        post("/auth/logout", request(next.path("refreshToken").asText()), 204);
        post("/auth/refresh", request(next.path("refreshToken").asText()), 401);
    }

    @Test void disabledAccountAndExpiredTokenCannotRefresh() throws Exception {
        String email = register();
        String refresh = login(email).path("refreshToken").asText();
        var user = users.findByEmail(email).orElseThrow();
        user.setEnabled(false); users.saveAndFlush(user);
        post("/auth/refresh", request(refresh), 401);
        user.setEnabled(true); users.saveAndFlush(user);
        var token = tokens.findAll().stream().filter(t -> t.getUser().getId().equals(user.getId())).findFirst().orElseThrow();
        token.setExpiresAt(Instant.now().minusSeconds(1)); tokens.saveAndFlush(token);
        post("/auth/refresh", request(refresh), 401);
        post("/auth/refresh", request("invalid"), 401);
    }

    @Test void simultaneousRefreshCanConsumeTokenOnlyOnce() throws Exception {
        String refresh = login(register()).path("refreshToken").asText();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> call = () -> {
                start.await();
                return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/refresh")
                    .contentType("application/json").content(request(refresh))).andReturn().getResponse().getStatus();
            };
            Future<Integer> first = pool.submit(call);
            Future<Integer> second = pool.submit(call);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200, 401);
        }
    }
}
