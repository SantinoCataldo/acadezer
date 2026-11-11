package com.acadezer.api.dto.response;

import com.acadezer.api.enums.Weekday;
import java.time.LocalTime;

public record ScheduleResponse(Long id, Weekday diaSemana, LocalTime horaInicio, LocalTime horaFin, int controlesAsistencia) {}
