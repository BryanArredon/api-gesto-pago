package com.proyecto.servicios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Usuario", description = "Informacion del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    @GetMapping("/perfil")
    @Operation(summary = "Obtener email del usuario autenticado")
    public ResponseEntity<Map<String, String>> perfil(Authentication authentication) {
        return ResponseEntity.ok(Map.of("email", authentication.getName()));
    }

    @GetMapping("/admin/ping")
    @Operation(summary = "Endpoint de prueba solo para administradores")
    public ResponseEntity<Map<String, String>> adminPing() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}