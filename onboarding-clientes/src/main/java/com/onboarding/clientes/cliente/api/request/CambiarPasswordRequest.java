package com.onboarding.clientes.cliente.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para cambio de contraseña")
public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        @Schema(example = "Segura123!")
        String contraseniaActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        @Schema(example = "NuevaClave456*")
        String nuevaContrasenia) {}
