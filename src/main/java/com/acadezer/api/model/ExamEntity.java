package com.acadezer.api.model;

import jakarta.persistence.*;
import com.acadezer.api.enums.ExamStatus;
import com.acadezer.api.enums.ExamType;
import lombok.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
public class ExamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ExamType tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ExamStatus estado;

    @Column(precision = 4, scale = 2)
    private BigDecimal calificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private SubjectEntity materia;
}
