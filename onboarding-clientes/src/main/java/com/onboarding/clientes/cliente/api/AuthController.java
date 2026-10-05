package com.onboarding.clientes.cliente.api;

import com.onboarding.clientes.cliente.api.request.LoginRequest;
import com.onboarding.clientes.cliente.application.command.ComandoLogin;
import com.onboarding.clientes.cliente.application.dto.LoginRespuesta;
import com.onboarding.clientes.cliente.application.service.ServicioAutenticacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el inicio de sesion y emision de tokens JWT.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y gestión de sesiones")
public class AuthController {

    private final ServicioAutenticacion servicioAutenticacion;

    public AuthController(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = servicioAutenticacion;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales, usuario activo y emite un token JWT de acceso y refresco.")
    @ApiResponse(responseCode = "200", description = "Autenticación exitosa")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @ApiResponse(responseCode = "403", description = "Usuario inactivo")
    @ApiResponse(responseCode = "423", description = "Usuario bloqueado por intentos fallidos")
    public ResponseEntity<LoginRespuesta> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigen = httpRequest.getRemoteAddr();
        String dispositivo = httpRequest.getHeader("User-Agent");

        ComandoLogin comando = new ComandoLogin(
                request.correo(), request.contrasenia(), ipOrigen, dispositivo);

        return ResponseEntity.ok(servicioAutenticacion.login(comando));
    }
}
