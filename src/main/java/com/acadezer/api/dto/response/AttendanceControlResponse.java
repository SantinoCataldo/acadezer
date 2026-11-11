package com.acadezer.api.dto.response;
import java.time.LocalDate;
import java.time.LocalTime;
public record AttendanceControlResponse(Long id, Long horarioId, LocalDate fecha, int numero,
                                        LocalTime horaInicio, LocalTime horaFin, String estado) {}
