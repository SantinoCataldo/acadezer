package com.acadezer.api.repository;

import com.acadezer.api.model.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    Optional<ScheduleEntity> findByIdAndMateriaIdAndMateriaUsuarioEmail(Long id, Long materiaId, String email);
}
