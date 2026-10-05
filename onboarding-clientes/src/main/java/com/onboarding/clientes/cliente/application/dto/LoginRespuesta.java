package com.onboarding.clientes.cliente.application.dto;

import java.util.UUID;

/** DTO que devuelve el resultado de una autenticacion exitosa con tokens JWT. */
public record LoginRespuesta(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID clienteId,
        UUID usuarioId) {}
