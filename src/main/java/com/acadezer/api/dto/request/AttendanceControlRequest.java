package com.acadezer.api.dto.request;
import com.acadezer.api.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
public record AttendanceControlRequest(@NotNull AttendanceStatus estado) {}
