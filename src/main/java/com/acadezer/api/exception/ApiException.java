package com.acadezer.api.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound() {
        return new ApiException(404, "Recurso no encontrado");
    }
}
