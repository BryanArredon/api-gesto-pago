package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import java.time.Instant;
import java.util.UUID;

/**
 * Proyeccion de lectura del usuario de acceso.
 *
 * <p>Nunca incluye el hash de la contrasena ni el del PIN: son secretos y no tienen por que viajar hacia el
 * cliente de la API. Se expone {@code tienePin} para que la interfaz sepa si debe ofrecer la opcion de
 * acceso por PIN o por biometria.
 */
public record UsuarioDto(
        UUID id,
        UUID clienteId,
        Correo correo,
        boolean activo,
        boolean tienePin,
        Instant ultimoAcceso,
        Instant creadoEn,
        Instant actualizadoEn) {}
