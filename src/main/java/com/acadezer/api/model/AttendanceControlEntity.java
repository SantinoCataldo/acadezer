package com.acadezer.api.model;

import com.acadezer.api.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "controles_asistencia", uniqueConstraints = @UniqueConstraint(name = "uk_control_horario_fecha_numero", columnNames = {"horario_id", "fecha", "numero"}))
@Getter @Setter @NoArgsConstructor
public class AttendanceControlEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "horario_id", nullable = false) private ScheduleEntity horario;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private int numero;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private AttendanceStatus estado;
}
