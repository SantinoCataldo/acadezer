package com.acadezer.api.exception;

import com.acadezer.api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> error(int status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(status, message, req.getRequestURI()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException ex, HttpServletRequest req) {
        return error(ex.getStatus(), ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return error(400, ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.joining(", ")), req);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class })
    ResponseEntity<ErrorResponse> input(Exception ex, HttpServletRequest req) {
        return error(400, "Solicitud inválida: revise campos, tipos y formato JSON", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> auth(AuthenticationException ex, HttpServletRequest req) {
        return error(401, "Credenciales inválidas", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest req) {
        return error(409, "Registro duplicado o conflicto de integridad", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> missing(Exception ex, HttpServletRequest req) {
        return error(404, "Recurso no encontrado", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> method(Exception ex, HttpServletRequest req) {
        return error(405, "Método no permitido", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> media(Exception ex, HttpServletRequest req) {
        return error(415, "Se requiere Content-Type application/json", req);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ErrorResponse> accept(Exception ex, HttpServletRequest req) {
        return error(406, "La respuesta está disponible en application/json", req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
        return error(500, "Error interno del servidor", req);
    }
}
