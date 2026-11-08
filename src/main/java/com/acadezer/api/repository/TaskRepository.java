package com.acadezer.api.repository;

import com.acadezer.api.model.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Optional<TaskEntity> findByIdAndMateriaUsuarioEmail(Long id, String email);

    List<TaskEntity> findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(Long materiaId, String email);

    void deleteByMateriaId(Long materiaId);

    List<TaskEntity> findByMateriaUsuarioEmailAndCompletadoFalseAndFechaLimiteBetweenOrderByFechaLimiteAscIdAsc(
            String email, LocalDate desde, LocalDate hasta);
}
