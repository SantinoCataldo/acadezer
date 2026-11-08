package com.acadezer.api.repository;

import com.acadezer.api.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.acadezer.api.enums.UserRole;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByEnabledTrue();

    long countByRole(UserRole role);
}
