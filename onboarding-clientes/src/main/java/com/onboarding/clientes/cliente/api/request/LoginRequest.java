package com.onboarding.clientes.cliente.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para inicio de sesión")
public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato de correo es inválido")
        @Schema(example = "juan.manzano@ejemplo.com")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(example = "Segura123!")
        String contrasenia) {}
