package com.acadezer.api.dto.response;

import java.time.LocalDate;
import com.acadezer.api.enums.AttendanceStatus;

public record AttendanceResponse(
        Long id,
        LocalDate fecha,
        AttendanceStatus estado,
        Long materiaId) {
}
