package com.acadezer.api.repository;

import com.acadezer.api.model.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {
    Optional<AttendanceEntity> findByIdAndMateriaUsuarioEmail(Long id, String email);

    List<AttendanceEntity> findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(Long materiaId, String email);

    void deleteByMateriaId(Long materiaId);

    boolean existsByMateriaIdAndFechaAndIdNot(Long materiaId, LocalDate fecha, Long id);
}
