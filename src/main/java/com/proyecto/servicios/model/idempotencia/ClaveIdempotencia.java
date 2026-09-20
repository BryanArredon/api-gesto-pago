package com.proyecto.servicios.model.idempotencia;

/**
 * Clave que identifica de forma unica una intencion de pago de un usuario.
 * Con ella se serializan duplicados y se reutiliza la fila en reintentos.
 */
public record ClaveIdempotencia(Long usuarioId, String idempotencyKey) {
}