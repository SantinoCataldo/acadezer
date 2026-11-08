package com.acadezer.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "apuntes")
@Getter
@Setter
@NoArgsConstructor
public class NoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 20000)
    private String contenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private SubjectEntity materia;
}
