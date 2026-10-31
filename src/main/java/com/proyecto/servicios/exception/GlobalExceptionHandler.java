package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> noLegible(HttpMessageNotReadableException e) {
        log.warn("Cuerpo de solicitud invalido o malformado: {}", e.getMessage());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.VAL_001,
                "El cuerpo de la solicitud es invalido, esta malformado o contiene valores no permitidos",
                traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
        String param = e.getName();
        log.warn("Tipo de parametro invalido: {}", param);
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.VAL_001,
                "El parametro '" + param + "' contiene un formato o tipo de dato no valido",
                traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> parametroFaltante(MissingServletRequestParameterException e) {
        log.warn("Parametro requerido faltante: {}", e.getParameterName());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.VAL_001,
                "El parametro requerido '" + e.getParameterName() + "' no fue proporcionado",
                traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        log.warn("Metodo HTTP no soportado: {}", e.getMethod());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.VAL_001,
                "Metodo HTTP '" + e.getMethod() + "' no soportado para este recurso",
                traceId());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> mediaTypeNoSoportado(HttpMediaTypeNotSupportedException e) {
        log.warn("Content-Type no soportado: {}", e.getContentType());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.VAL_001,
                "Tipo de contenido '" + e.getContentType() + "' no soportado",
                traceId());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException e) {
        log.warn("Acceso denegado: {}", e.getMessage());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.PERM_001,
                "No tiene autorizacion para ejecutar esta operacion",
                traceId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException e) {
        log.warn("Fallo de autenticacion: {}", e.getMessage());
        ErrorResponse body = new ErrorResponse(
                ErrorCodes.AUTH_005,
                "Autenticacion requerida o token invalido",
                traceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
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