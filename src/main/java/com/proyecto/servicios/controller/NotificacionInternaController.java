package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.notificacion.TipoNotificacion;
import com.proyecto.servicios.model.notificacion.NotificacionInternaRequest;
import com.proyecto.servicios.service.notificacion.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/interno/notificaciones")
@Tag(name = "Notificaciones internas", description = "Ingreso de notificaciones desde otros microservicios")
@Slf4j
public class NotificacionInternaController {

    private static final String HEADER_TOKEN = "X-Internal-Token";

    private final NotificacionService notificacionService;
    private final String tokenEsperado;

    public NotificacionInternaController(
            NotificacionService notificacionService,
            @Value("${notificaciones.interno.token:}") String tokenEsperado) {
        this.notificacionService = notificacionService;
        this.tokenEsperado = tokenEsperado;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar una notificacion interna de otro microservicio")
    public ResponseEntity<Void> registrar(
            @RequestHeader(value = HEADER_TOKEN, required = false) String token,
            @RequestBody NotificacionInternaRequest request) {
        if (!tokenValido(token)) {
            log.warn("Intento de notificacion interna con token invalido");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        notificacionService.registrarPorEmail(
                request.email(),
                TipoNotificacion.desde(request.tipo()),
                request.titulo(),
                request.cuerpo(),
                request.datos(),
                request.dedupeKey(),
                request.correo());
        return ResponseEntity.noContent().build();
    }

    private boolean tokenValido(String recibido) {
        if (!StringUtils.hasText(tokenEsperado) || !StringUtils.hasText(recibido)) {
            return false;
        }
        return MessageDigest.isEqual(
                tokenEsperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }
}
