package com.proyecto.servicios.service.notificacion;

import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.evento.NotificacionSolicitadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
@Slf4j
public class NotificacionEventoListener {

    private final NotificacionService notificacionService;
    private final NotificacionEmailService emailService;

    public NotificacionEventoListener(
            NotificacionService notificacionService,
            NotificacionEmailService emailService) {
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    @Async("notificacionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alSolicitarNotificacion(NotificacionSolicitadaEvent evento) {
        Optional<Notificacion> creada;
        try {
            creada = notificacionService.registrarPorEmail(
                    evento.email(),
                    evento.tipo(),
                    evento.titulo(),
                    evento.cuerpo(),
                    evento.datos(),
                    evento.dedupeKey(),
                    evento.correo());
        } catch (DataIntegrityViolationException e) {
            log.debug("Notificacion duplicada para clave {}", evento.dedupeKey());
            return;
        } catch (Exception e) {
            log.error("No se pudo registrar la notificacion solicitada para {}", evento.email(), e);
            return;
        }
        creada.ifPresent(notificacion -> {
            notificacionService.enviarEnTiempoReal(notificacion);
            if (notificacion.getEmailEstado() == EstadoCorreoNotificacion.PENDIENTE) {
                emailService.enviarAsync(notificacion.getId());
            }
        });
    }
}
