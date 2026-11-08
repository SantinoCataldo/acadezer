package com.acadezer.api.dto.response;

import com.acadezer.api.enums.UserRole;

public record UserResponse(
        Long id,
        String nombre,
        String email,
        UserRole role,
        boolean enabled) {
}
