package com.proyecto.servicios.service.notificacion;

import com.proyecto.servicios.model.notificacion.NotificacionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificacionBroadcaster {

    public static final String DESTINO_COLA = "/queue/notificaciones";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificacionBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void enviar(String email, NotificacionDto notificacion) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(email, DESTINO_COLA, notificacion);
        } catch (Exception e) {
            log.warn("No se pudo entregar la notificacion en tiempo real a {}: {}", email, e.getMessage());
        }
    }
}
