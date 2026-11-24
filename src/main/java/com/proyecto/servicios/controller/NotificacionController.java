package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.notificacion.NotificacionPaginaDto;
import com.proyecto.servicios.service.notificacion.NotificacionService;
import com.proyecto.servicios.service.security.ServicioIdentidadSso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notificaciones")
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final ServicioIdentidadSso servicioIdentidadSso;

    public NotificacionController(
            NotificacionService notificacionService,
            ServicioIdentidadSso servicioIdentidadSso) {
        this.notificacionService = notificacionService;
        this.servicioIdentidadSso = servicioIdentidadSso;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar la bandeja de notificaciones del usuario")
    public ResponseEntity<NotificacionPaginaDto> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            Authentication authentication) {
        return ResponseEntity.ok(notificacionService.listar(
                usuarioId(authentication), pagina, tamanio, soloNoLeidas));
    }

    @GetMapping(value = "/no-leidas/contador", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Contar las notificaciones no leidas del usuario")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "noLeidas", notificacionService.contarNoLeidas(usuarioId(authentication))));
    }

    @PatchMapping(value = "/{id}/leida", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Marcar una notificacion como leida")
    public ResponseEntity<Map<String, Object>> marcarLeida(
            @PathVariable Long id, Authentication authentication) {
        boolean actualizada = notificacionService.marcarLeida(usuarioId(authentication), id);
        if (!actualizada) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("id", id, "leida", true));
    }

    @PatchMapping(value = {"/leidas", "/leer-todas"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Marcar todas las notificaciones del usuario como leidas")
    public ResponseEntity<Map<String, Integer>> marcarTodasLeidas(Authentication authentication) {
        int actualizadas = notificacionService.marcarTodasLeidas(usuarioId(authentication));
        return ResponseEntity.ok(Map.of("actualizadas", actualizadas));
    }

    private Long usuarioId(Authentication authentication) {
        return servicioIdentidadSso.obtenerOCrearUsuarioId(authentication);
    }
}
