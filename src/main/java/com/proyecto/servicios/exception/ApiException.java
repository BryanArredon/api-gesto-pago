package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ApiException authInvalida() {
        return new ApiException(ErrorCodes.AUTH_001, "Credenciales invalidas", HttpStatus.UNAUTHORIZED);
    }

    public static ApiException cuentaInactiva() {
        return new ApiException(ErrorCodes.AUTH_002, "La cuenta esta inactiva", HttpStatus.FORBIDDEN);
    }

    public static ApiException refreshInvalido() {
        return new ApiException(ErrorCodes.AUTH_003, "Refresh token invalido o expirado", HttpStatus.UNAUTHORIZED);
    }

    public static ApiException tokenInvalido() {
        return new ApiException(ErrorCodes.AUTH_004, "Token invalido o expirado", HttpStatus.UNAUTHORIZED);
    }

    public static ApiException noAutenticado() {
        return new ApiException(ErrorCodes.AUTH_005, "Autenticacion requerida", HttpStatus.UNAUTHORIZED);
    }

    public static ApiException sinPermisos() {
        return new ApiException(ErrorCodes.PERM_001, "No tiene permisos para este recurso", HttpStatus.FORBIDDEN);
    }

    public static ApiException limiteAlcanzado() {
        return new ApiException(ErrorCodes.RATE_001,
                "Demasiadas peticiones, intente mas tarde", HttpStatus.TOO_MANY_REQUESTS);
    }

    public static ApiException emailYaRegistrado() {
        return new ApiException(ErrorCodes.AUTH_006, "El correo ya esta registrado", HttpStatus.CONFLICT);
    }

    public static ApiException referenciaInvalida() {
        return new ApiException(ErrorCodes.PAGO_001, "La referencia no es valida", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ApiException referenciaNoVerificable() {
        return new ApiException(ErrorCodes.PAGO_002, "El servicio no soporta verificacion de referencia", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ApiException productoNoDisponible() {
        return new ApiException(ErrorCodes.PAGO_003, "El producto no esta disponible en el catalogo", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ApiException montoInvalido() {
        return new ApiException(ErrorCodes.PAGO_004, "El monto es invalido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ApiException confirmacionNoPermitida() {
        return new ApiException(ErrorCodes.PAGO_005, "La transaccion no esta en estado de confirmacion", HttpStatus.CONFLICT);
    }
}