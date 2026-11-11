package com.acadezer.api.repository;

import com.acadezer.api.model.AttendanceControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.*;

public interface AttendanceControlRepository extends JpaRepository<AttendanceControlEntity, Long> {
    boolean existsByHorarioId(Long horarioId);
    Optional<AttendanceControlEntity> findByHorarioIdAndFechaAndNumero(Long horarioId, LocalDate fecha, int numero);
    List<AttendanceControlEntity> findByHorarioMateriaIdAndFechaBetween(Long materiaId, LocalDate desde, LocalDate hasta);
    void deleteByHorarioMateriaId(Long materiaId);
    @Query("select count(c) from AttendanceControlEntity c where c.horario.materia.id = :materiaId and c.fecha > :fechaFin")
    long countAfterDate(Long materiaId, LocalDate fechaFin);
}
