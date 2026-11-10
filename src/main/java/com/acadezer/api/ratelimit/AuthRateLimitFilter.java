package com.acadezer.api.ratelimit;

import com.acadezer.api.dto.response.ErrorResponse;
import com.acadezer.api.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Installed only in the security chain, after CORS and before authentication. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final AuthRequestLimiter limits;
    private final ClientIpResolver ips;
    private final JsonMapper json;

    public AuthRateLimitFilter(AuthRequestLimiter limits, ClientIpResolver ips, JsonMapper json) {
        this.limits = limits;
        this.ips = ips;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod())) {
            String path = request.getServletPath();
            // MockMvc may leave servletPath empty; both are container-parsed paths.
            if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
            try {
                if ("/auth".equals(path) || "/auth/refresh".equals(path) || "/auth/logout".equals(path)) limits.loginIp(ips.resolve(request));
                else if ("/auth/register".equals(path)) limits.registerIp(ips.resolve(request));
            } catch (RateLimitExceededException ex) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(ex.getRetryAfterSeconds()));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(json.writeValueAsString(
                    ErrorResponse.of(429, ex.getMessage(), request.getRequestURI())));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
