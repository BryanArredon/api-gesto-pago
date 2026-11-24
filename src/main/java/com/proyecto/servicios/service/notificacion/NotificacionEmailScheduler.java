package com.proyecto.servicios.service.notificacion;

import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.repositorys.notificacion.NotificacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "notificaciones.correo", name = "reintentos-habilitados",
        havingValue = "true", matchIfMissing = true)
public class NotificacionEmailScheduler {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionEmailService emailService;
    private final int maxIntentos;
    private final int lote;

    public NotificacionEmailScheduler(
            NotificacionRepository notificacionRepository,
            NotificacionEmailService emailService,
            @Value("${notificaciones.correo.max-intentos:3}") int maxIntentos,
            @Value("${notificaciones.correo.lote-reintento:50}") int lote) {
        this.notificacionRepository = notificacionRepository;
        this.emailService = emailService;
        this.maxIntentos = maxIntentos;
        this.lote = Math.max(lote, 1);
    }

    @Scheduled(
            fixedDelayString = "${notificaciones.correo.reintento-ms:60000}",
            initialDelayString = "${notificaciones.correo.reintento-inicial-ms:30000}")
    public void reintentarCorreos() {
        Instant ahora = Instant.now();
        notificacionRepository.liberarEnviosAtascados(
                EstadoCorreoNotificacion.ENVIANDO,
                EstadoCorreoNotificacion.FALLIDO,
                ahora.minus(Duration.ofMinutes(10)),
                ahora);

        List<Long> pendientes = notificacionRepository.buscarPendientesEnvio(
                List.of(EstadoCorreoNotificacion.PENDIENTE, EstadoCorreoNotificacion.FALLIDO),
                maxIntentos,
                ahora.minus(Duration.ofMinutes(2)),
                PageRequest.of(0, lote));
        if (pendientes.isEmpty()) {
            return;
        }
        log.info("Reintentando {} correos de notificacion", pendientes.size());
        pendientes.forEach(emailService::enviarAsync);
    }
}
