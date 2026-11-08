package com.acadezer.api.repository;

import com.acadezer.api.model.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {
    Optional<SubjectEntity> findByIdAndUsuarioEmail(Long id, String email);

    List<SubjectEntity> findByUsuarioEmailOrderByIdAsc(String email);
}
