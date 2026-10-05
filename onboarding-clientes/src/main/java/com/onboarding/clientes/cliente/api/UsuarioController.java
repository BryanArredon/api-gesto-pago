package com.onboarding.clientes.cliente.api;

import com.onboarding.clientes.cliente.api.request.CambiarPasswordRequest;
import com.onboarding.clientes.cliente.application.command.ComandoCambiarPassword;
import com.onboarding.clientes.cliente.application.dto.UsuarioDto;
import com.onboarding.clientes.cliente.application.service.ServicioAutenticacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestion de usuarios de acceso.
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Consultas de usuario y gestion de contrasena")
public class UsuarioController {

    private final ServicioAutenticacion servicioAutenticacion;

    public UsuarioController(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario por ID")
    public ResponseEntity<UsuarioDto> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioAutenticacion.obtenerPorId(id));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Cambiar contrasena de acceso", description = "Valida la contrasena actual y las reglas de complejidad de la nueva contrasena.")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarPasswordRequest request) {

        ComandoCambiarPassword comando = new ComandoCambiarPassword(
                id, request.contraseniaActual(), request.nuevaContrasenia());
        servicioAutenticacion.cambiarContrasenia(comando);
        return ResponseEntity.noContent().build();
    }
}
