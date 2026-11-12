package com.acadezer.api.enums;

/**
 * Tipo de evaluación de una materia. El valor se guarda tal cual en la base
 * (String) para que un enum nuevo no rompa las filas ya cargadas.
 */
public enum ExamType {
    PARCIAL_1,
    PARCIAL_2,
    FINAL,
    RECUPERATORIO,
    TP
}
