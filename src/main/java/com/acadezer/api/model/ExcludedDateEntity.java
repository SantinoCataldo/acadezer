package com.acadezer.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "fechas_sin_cursada", uniqueConstraints = @UniqueConstraint(name = "uk_fecha_sin_cursada", columnNames = {"materia_id", "fecha"}))
@Getter @Setter @NoArgsConstructor
public class ExcludedDateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "materia_id", nullable = false) private SubjectEntity materia;
    @Column(nullable = false) private LocalDate fecha;
}
