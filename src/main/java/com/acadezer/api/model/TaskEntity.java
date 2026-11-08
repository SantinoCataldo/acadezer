package com.acadezer.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="pendientes")
@Getter
@Setter
@NoArgsConstructor
public class TaskEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=200)
    private String titulo;

    @Column(length=2000)
    private String descripcion;
    private LocalDate fechaLimite;

    @Column(nullable=false)
    private boolean completado;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="materia_id", nullable=false)
    private SubjectEntity materia;
}
