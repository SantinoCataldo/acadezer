package com.acadezer.api.repository;

import com.acadezer.api.model.ExcludedDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface ExcludedDateRepository extends JpaRepository<ExcludedDateEntity, Long> {
    Optional<ExcludedDateEntity> findByMateriaIdAndFecha(Long materiaId, LocalDate fecha);
    List<ExcludedDateEntity> findByMateriaIdAndFechaBetweenOrderByFecha(Long materiaId, LocalDate desde, LocalDate hasta);
    void deleteByMateriaId(Long materiaId);
}
