package com.proyecto.servicios.evento;

import com.proyecto.servicios.entity.notificacion.TipoNotificacion;

import java.util.Map;

public record NotificacionSolicitadaEvent(
        String email,
        TipoNotificacion tipo,
        String titulo,
        String cuerpo,
        Map<String, Object> datos,
        String dedupeKey,
        boolean correo) {
}
