package com.proyecto.servicios.model.notificacion;

import java.util.List;

public record NotificacionPaginaDto(
        List<NotificacionDto> items,
        long total,
        long noLeidas,
        int pagina,
        int tamanio) {
}
