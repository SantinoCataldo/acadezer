package com.acadezer.api.repository;

import com.acadezer.api.model.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;

public interface NoteRepository extends JpaRepository<NoteEntity, Long> {
    Optional<NoteEntity> findByIdAndMateriaUsuarioEmail(Long id, String email);

    List<NoteEntity> findByMateriaIdAndMateriaUsuarioEmailOrderByIdAsc(Long materiaId, String email);

    void deleteByMateriaId(Long materiaId);

}
