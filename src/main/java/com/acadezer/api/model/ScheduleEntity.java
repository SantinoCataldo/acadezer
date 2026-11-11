package com.acadezer.api.model;

import com.acadezer.api.enums.Weekday;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity @Table(name = "horarios") @Getter @Setter @NoArgsConstructor
public class ScheduleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "materia_id", nullable = false) private SubjectEntity materia;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private Weekday diaSemana;
    @Column(nullable = false) private LocalTime horaInicio;
    @Column(nullable = false) private LocalTime horaFin;
    @Column(nullable = false) private int controlesAsistencia;
}
