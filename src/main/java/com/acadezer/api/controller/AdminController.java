package com.acadezer.api.controller;

import com.acadezer.api.dto.request.UserEnabledRequest;
import com.acadezer.api.dto.response.AdminStatsResponse;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService service;

    @GetMapping("/users")
    public List<UserResponse> users() {
        return service.listUsers();
    }

    @PatchMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody UserEnabledRequest request) {
        return service.setEnabled(id, request.enabled());
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return service.stats();
    }
}
