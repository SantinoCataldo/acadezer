package com.acadezer.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

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

    /** Marca de creación: se escribe una sola vez y el front la muestra como referencia. */
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Última modificación: se refresca en cada guardado. */
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private SubjectEntity materia;

    @PrePersist
    @PreUpdate
    void refreshTimestamps() {
        Instant now = Instant.now();
        if (creadoEn == null) {
            creadoEn = now;
        }
        actualizadoEn = now;
    }
}
