package com.onboarding.clientes.shared.evento;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de negocio inmutable para auditoria.
 *
 * @param tipo nombre del evento, por ejemplo {@code cliente.registrado}
 * @param agregadoTipo tipo de agregado
 * @param agregadoId identificador del agregado
 * @param carga datos relevantes del evento (nunca contrasenas ni tokens)
 * @param correlationId traza de la peticion que lo produjo
 * @param actor usuario o sistema responsable
 * @param instante momento del evento
 */
public record EventoDominio(
        UUID id,
        String tipo,
        String agregadoTipo,
        UUID agregadoId,
        Map<String, Object> carga,
        String correlationId,
        String actor,
        Instant instante) {

    public static final String TIPO_CLIENTE = "cliente";
    public static final String TIPO_USUARIO = "usuario";
    public static final String TIPO_CUENTA = "cuenta";

    public static EventoDominio crear(
            String tipo, String agregadoTipo, UUID agregadoId, Map<String, Object> carga, String actor, Instant instante) {
        Map<String, Object> segura = carga == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(carga));
        return new EventoDominio(UUID.randomUUID(), tipo, agregadoTipo, agregadoId, segura, null, actor, instante);
    }

    public EventoDominio conCorrelationId(String correlationId) {
        return new EventoDominio(id, tipo, agregadoTipo, agregadoId, carga, correlationId, actor, instante);
    }
}
