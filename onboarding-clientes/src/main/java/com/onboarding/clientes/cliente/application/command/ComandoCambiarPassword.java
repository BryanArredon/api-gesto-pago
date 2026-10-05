package com.onboarding.clientes.cliente.application.command;

import java.util.UUID;

/** Comando para cambiar la contrasena de un usuario. */
public record ComandoCambiarPassword(
        UUID usuarioId,
        String contraseniaActual,
        String nuevaContrasenia) {}
