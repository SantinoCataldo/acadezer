package com.acadezer.api.controller;

import com.acadezer.api.dto.request.RegisterRequest;
import com.acadezer.api.dto.request.AuthRequest;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.dto.response.AuthResponse;
import com.acadezer.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @GetMapping("/me")
    public UserResponse currentUser(java.security.Principal principal) {
        return auth.currentUser(principal.getName());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest input) {
        return ResponseEntity.status(201).body(auth.register(input));
    }

    @PostMapping
    public AuthResponse login(@Valid @RequestBody AuthRequest input) {
        return auth.login(input);
    }
}
