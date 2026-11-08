package com.acadezer.api.mapper;

import com.acadezer.api.dto.request.RegisterRequest;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.model.UserEntity;
import com.acadezer.api.enums.UserRole;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class UserMapper {
      public UserEntity toEntity(RegisterRequest request) {
        UserEntity entity = new UserEntity();
        entity.setNombre(request.nombre().trim());
        entity.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        entity.setRole(UserRole.STUDENT);
        entity.setEnabled(true);
        return entity;
    }

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(entity.getId(), entity.getNombre(), entity.getEmail(), entity.getRole(), entity.isEnabled());
    }
}
