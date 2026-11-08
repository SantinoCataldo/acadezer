package com.acadezer.api.dto.response;

public record AdminStatsResponse(
    long usuarios,
    long usuariosHabilitados,
    long estudiantes,
    long administradores,
    long materias,
    long apuntes,
    long pendientes,
    long evaluaciones,
    long asistencias
) {}
