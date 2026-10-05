package com.onboarding.clientes.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Catalogo cerrado de errores de negocio.
 *
 * <p>Cada error tiene un codigo estable (contrato con el cliente, nunca se cambia aunque cambie el mensaje
 * en español) y un codigo HTTP coherente. Centralizarlo aqui evita que el cliente tenga que interpretar
 * textos libres y permite documentar la API automaticamente.
 */
public enum CodigoError {

    // --- Cliente: reglas de negocio (un proceso de negocio fallo, los datos estan bien formados) ---
    CLIENTE_YA_REGISTRADO("cliente_ya_registrado", HttpStatus.CONFLICT),
    CLIENTE_NO_ENCONTRADO("cliente_no_encontrado", HttpStatus.NOT_FOUND),
    CLIENTE_INACTIVO("cliente_inactivo", HttpStatus.CONFLICT),
    CLIENTE_MENOR_DE_EDAD("cliente_menor_de_edad", HttpStatus.UNPROCESSABLE_ENTITY),
    CURP_DUPLICADA("curp_duplicada", HttpStatus.CONFLICT),
    RFC_DUPLICADO("rfc_duplicado", HttpStatus.CONFLICT),
    CORREO_DUPLICADO("correo_duplicado", HttpStatus.CONFLICT),

    // --- Cliente: validacion de datos ---
    CURP_INVALIDA("curp_invalida", HttpStatus.BAD_REQUEST),
    RFC_INVALIDO("rfc_invalido", HttpStatus.BAD_REQUEST),
    DATO_INVALIDO("dato_invalido", HttpStatus.BAD_REQUEST),
    SALDO_INVALIDO("saldo_invalido", HttpStatus.UNPROCESSABLE_ENTITY),
    EDAD_MINIMA_NO_CUMPLIDA("edad_minima_no_cumplida", HttpStatus.UNPROCESSABLE_ENTITY),

    // --- Cuenta ---
    CUENTA_NO_ENCONTRADA("cuenta_no_encontrada", HttpStatus.NOT_FOUND),
    CUENTA_SIN_CLIENTE_ACTIVO("cuenta_sin_cliente_activo", HttpStatus.UNPROCESSABLE_ENTITY),
    NUMERO_CUENTA_INVALIDO("numero_cuenta_invalido", HttpStatus.BAD_REQUEST),

    // --- Usuario / autenticacion ---
    USUARIO_NO_ENCONTRADO("usuario_no_encontrado", HttpStatus.NOT_FOUND),
    USUARIO_INACTIVO("usuario_inactivo", HttpStatus.FORBIDDEN),
    CREDENCIALES_INVALIDAS("credenciales_invalidas", HttpStatus.UNAUTHORIZED),
    CONTRASENA_INVALIDA("contrasena_invalida", HttpStatus.BAD_REQUEST),
    CORREO_DUPLICADO_USUARIO("correo_duplicado_usuario", HttpStatus.CONFLICT),
    PIN_NO_CONFIGURADO("pin_no_configurado", HttpStatus.UNPROCESSABLE_ENTITY),
    USUARIO_BLOQUEADO("usuario_bloqueado", HttpStatus.LOCKED),

    // --- Tokens y dispositivos ---
    TOKEN_INVALIDO("token_invalido", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRADO("token_expirado", HttpStatus.UNAUTHORIZED),
    TOKEN_REUTILIZADO("token_reutilizado", HttpStatus.UNAUTHORIZED),
    DISPOSITIVO_NO_ENCONTRADO("dispositivo_no_encontrado", HttpStatus.NOT_FOUND),
    DISPOSITIVO_YA_REGISTRADO("dispositivo_ya_registrado", HttpStatus.CONFLICT),
    DESAFIO_BIOMETRICO_INVALIDO("desafio_biometrico_invalido", HttpStatus.UNAUTHORIZED),
    CLAVE_PUBLICA_INVALIDA("clave_publica_invalida", HttpStatus.BAD_REQUEST),

    // --- Seguridad / transaccional ---
    ACCESO_DENEGADO("acceso_denegado", HttpStatus.FORBIDDEN),
    NO_AUTENTICADO("no_autenticado", HttpStatus.UNAUTHORIZED),
    CONFLICTO_CONCURRENTE("conflicto_concurrente", HttpStatus.CONFLICT),

    // --- Transversales ---
    SOLICITUD_INVALIDA("solicitud_invalida", HttpStatus.BAD_REQUEST),
    RECURSO_NO_ENCONTRADO("recurso_no_encontrado", HttpStatus.NOT_FOUND),
    METODO_NO_PERMITIDO("metodo_no_permitido", HttpStatus.METHOD_NOT_ALLOWED),
    ERROR_INTERNO("error_interno", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String codigo;
    private final HttpStatus estado;

    CodigoError(String codigo, HttpStatus estado) {
        this.codigo = codigo;
        this.estado = estado;
    }

    /** @return codigo estable expuesto al cliente */
    public String codigo() {
        return codigo;
    }

    public HttpStatus estado() {
        return estado;
    }
}
