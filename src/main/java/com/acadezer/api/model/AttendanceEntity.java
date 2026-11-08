package com.acadezer.api.model;

import jakarta.persistence.*;
import com.acadezer.api.enums.AttendanceStatus;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "asistencias", uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_materia_fecha", columnNames = {
        "materia_id", "fecha" }))
@Getter
@Setter
@NoArgsConstructor
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AttendanceStatus estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private SubjectEntity materia;
}
