package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void manejaApiExceptionCorrectamente() {
        ApiException ex = ApiException.saldoInsuficiente();
        ResponseEntity<ErrorResponse> response = handler.apiException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAGO-010", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("Saldo disponible insuficiente"));
    }

    @Test
    void manejaValidacionDeCampos400() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "pagoRequestDto");
        bindingResult.addError(new FieldError("pagoRequestDto", "numeroCuenta", "numeroCuenta es obligatorio"));
        MethodParameter param = new MethodParameter(this.getClass().getDeclaredMethod("setUp"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.validacion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("numeroCuenta"));
    }

    @Test
    void manejaJsonMalformado400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("bad json".getBytes()));

        ResponseEntity<ErrorResponse> response = handler.noLegible(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("invalido"));
    }

    @Test
    void manejaTipoDeDatoInvalido400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, null);

        ResponseEntity<ErrorResponse> response = handler.tipoInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("id"));
    }

    @Test
    void manejaParametroFaltante400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("idServicio", "Integer");

        ResponseEntity<ErrorResponse> response = handler.parametroFaltante(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("idServicio"));
    }

    @Test
    void manejaMetodoHttpNoSoportado405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

        ResponseEntity<ErrorResponse> response = handler.metodoNoSoportado(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
    }

    @Test
    void manejaMediaTypeNoSoportado415() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponse> response = handler.mediaTypeNoSoportado(ex);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals(ErrorCodes.VAL_001, response.getBody().getCode());
    }

    @Test
    void manejaAccesoDenegado403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = handler.accesoDenegado(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ErrorCodes.PERM_001, response.getBody().getCode());
    }

    @Test
    void manejaFalloAutenticacion401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.autenticacion(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(ErrorCodes.AUTH_005, response.getBody().getCode());
    }

    @Test
    void manejaErrorGenericoSinExponerTraza500() {
        RuntimeException ex = new RuntimeException("Error en base de datos interna o SQL sensible");

        ResponseEntity<ErrorResponse> response = handler.generico(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ErrorCodes.UNK_001, response.getBody().getCode());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
    }
}
