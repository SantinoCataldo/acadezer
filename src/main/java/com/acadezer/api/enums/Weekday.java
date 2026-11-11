package com.acadezer.api.enums;

public enum Weekday {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;
    public static Weekday from(java.time.DayOfWeek day) { return values()[day.getValue() - 1]; }
}
