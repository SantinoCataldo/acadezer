package com.acadezer.api.ratelimit;

import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthRequestLimiter {
    private final FixedWindowRateLimiter counters;
    private final int loginAccount;
    private final int loginIp;
    private final Duration loginWindow;
    private final int registerAccount;
    private final int registerIp;
    private final Duration registerWindow;

    public AuthRequestLimiter(
            @Value("${app.limits.max-keys}") int maxKeys,
            @Value("${app.limits.login.account}") int loginAccount,
            @Value("${app.limits.login.ip}") int loginIp,
            @Value("${app.limits.login.window}") Duration loginWindow,
            @Value("${app.limits.register.account}") int registerAccount,
            @Value("${app.limits.register.ip}") int registerIp,
            @Value("${app.limits.register.window}") Duration registerWindow) {
        if (loginAccount < 1 || loginIp < 1 || registerAccount < 1 || registerIp < 1
                || loginWindow.isZero() || loginWindow.isNegative()
                || registerWindow.isZero() || registerWindow.isNegative()) {
            throw new IllegalArgumentException("La configuración de límites de autenticación debe ser positiva");
        }
        this.counters = new FixedWindowRateLimiter(maxKeys);
        this.loginAccount = loginAccount;
        this.loginIp = loginIp;
        this.loginWindow = loginWindow;
        this.registerAccount = registerAccount;
        this.registerIp = registerIp;
        this.registerWindow = registerWindow;
    }

    public void loginIp(String ip) {
        counters.acquire("login:ip:" + ip, loginIp, loginWindow);
    }

    public void registerIp(String ip) {
        counters.acquire("register:ip:" + ip, registerIp, registerWindow);
    }

    public void loginAccount(String email) {
        counters.acquire("login:account:" + normalize(email), loginAccount, loginWindow);
    }

    public void registerAccount(String email) {
        counters.acquire("register:account:" + normalize(email), registerAccount, registerWindow);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
