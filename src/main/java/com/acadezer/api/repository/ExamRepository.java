package com.acadezer.api.repository;

import com.acadezer.api.model.ExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;

public interface ExamRepository extends JpaRepository<ExamEntity, Long> {
    Optional<ExamEntity> findByIdAndMateriaUsuarioEmail(Long id, String email);

    List<ExamEntity> findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(Long materiaId, String email);

    void deleteByMateriaId(Long materiaId);

}
