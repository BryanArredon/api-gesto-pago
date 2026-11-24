package com.proyecto.servicios.service.notificacion;

import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.evento.MovimientoRegistradoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
@Slf4j
public class MovimientoNotificacionListener {

    private final NotificacionService notificacionService;
    private final NotificacionEmailService emailService;

    public MovimientoNotificacionListener(
            NotificacionService notificacionService,
            NotificacionEmailService emailService) {
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    @Async("notificacionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRegistrarMovimiento(MovimientoRegistradoEvent evento) {
        Optional<Notificacion> creada;
        try {
            creada = notificacionService.registrarMovimiento(evento);
        } catch (DataIntegrityViolationException e) {
            log.debug("Notificacion duplicada para transaccion {} estado {}",
                    evento.transaccionId(), evento.estado());
            return;
        } catch (Exception e) {
            log.error("No se pudo registrar la notificacion de la transaccion {}", evento.transaccionId(), e);
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
