package com.acadezer.api;
import com.acadezer.api.repository.UserRepository;
import com.acadezer.api.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RefreshTokenApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;
    private String account() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/auth/register").contentType("application/json").content("{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"password123\"}")) .andExpect(status().isCreated());
        return email;
    }
    private String login(String email) throws Exception {
        var result = mvc.perform(post("/auth").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("HttpOnly", "Secure", "SameSite=None", "Path=/auth");
        return header.split(";", 2)[0].split("=", 2)[1];
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refresh(String value) {
        return post("/auth/refresh").cookie(new Cookie("acadezer_refresh", value)).header("Origin", "http://localhost:4200").header("X-CSRF-Protection", "1");
    }
    @Test void rotatesAndLogoutRevokes() throws Exception {
        String token = login(account());
        var result = mvc.perform(refresh(token).header("Authorization", "Bearer expired-or-invalid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        String next = result.getResponse().getHeader("Set-Cookie").split(";", 2)[0].split("=", 2)[1];
        assertThat(next).isNotEqualTo(token);
        mvc.perform(refresh(token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").cookie(new Cookie("acadezer_refresh", next)).header("Origin", "http://localhost:4200").header("X-CSRF-Protection", "1"))
            .andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(refresh(next)).andExpect(status().isUnauthorized());
    }
    @Test void rejectsCsrfAndUntrustedOrigin() throws Exception {
        String token = login(account());
        mvc.perform(post("/auth/refresh").cookie(new Cookie("acadezer_refresh", token))).andExpect(status().isForbidden());
        mvc.perform(post("/auth/refresh").cookie(new Cookie("acadezer_refresh", token)).header("Origin", "https://evil.example").header("X-CSRF-Protection", "1")).andExpect(status().isForbidden());
        mvc.perform(refresh(token)).andExpect(status().isOk());
    }
    @Test void corsAllowsOnlyConfiguredOriginAndCredentials() throws Exception {
        mvc.perform(options("/auth/refresh").header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-Protection"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/auth/refresh").header("Origin", "https://evil.example")
            .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-Protection"))
            .andExpect(status().isForbidden());
    }
    @Test void disabledAndExpiredSessionsCannotRefresh() throws Exception {
        String email = account();
        String token = login(email);
        var user = users.findByEmail(email).orElseThrow();
        user.setEnabled(false); users.saveAndFlush(user);
        mvc.perform(refresh(token)).andExpect(status().isUnauthorized());
        user.setEnabled(true); users.saveAndFlush(user);
        var session = tokens.findAll().stream().filter(t -> t.getUser().getId().equals(user.getId())).findFirst().orElseThrow();
        session.setExpiresAt(java.time.Instant.now().minusSeconds(1)); tokens.saveAndFlush(session);
        mvc.perform(refresh(token)).andExpect(status().isUnauthorized());
    }
    @Test void simultaneousRefreshCanConsumeTokenOnlyOnce() throws Exception {
        String token = login(account());
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> call = () -> { start.await(); return mvc.perform(refresh(token)).andReturn().getResponse().getStatus(); };
            var first = pool.submit(call); var second = pool.submit(call); start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 401);
        }
    }
}
