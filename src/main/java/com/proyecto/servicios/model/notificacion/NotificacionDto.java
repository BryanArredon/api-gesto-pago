package com.proyecto.servicios.model.notificacion;

import java.time.Instant;
import java.util.Map;

public record NotificacionDto(
        Long id,
        String tipo,
        String titulo,
        String cuerpo,
        Long transaccionId,
        Map<String, Object> datos,
        boolean leida,
        Instant leidaEn,
        Instant createdAt) {
}
