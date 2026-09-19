package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> apiException(ApiException e) {
        log.warn("Error de negocio code={} message={}", e.getCode(), e.getMessage());
        ErrorResponse body = new ErrorResponse(e.getCode(), e.getMessage(), traceId());
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        String detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Error de validacion: {}", detalles);
        ErrorResponse body = new ErrorResponse(ErrorCodes.VAL_001, detalles, traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception e) {
        log.error("Error no controlado", e);
        ErrorResponse body = new ErrorResponse(ErrorCodes.UNK_001,
                "Error interno del servidor", traceId());
        return ResponseEntity.internalServerError().body(body);
    }

    private String traceId() {
        String tid = MDC.get("traceId");
        return tid == null ? null : tid;
    }
}