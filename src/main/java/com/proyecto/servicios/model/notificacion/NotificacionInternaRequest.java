package com.proyecto.servicios.model.notificacion;

import java.util.Map;

public record NotificacionInternaRequest(
        String email,
        String tipo,
        String titulo,
        String cuerpo,
        String dedupeKey,
        Map<String, Object> datos,
        boolean correo) {
}
