package com.onboarding.clientes.cliente.application.command;

/** Comando con las credenciales necesarias para iniciar sesion. */
public record ComandoLogin(
        String correo,
        String contrasenia,
        String ipOrigen,
        String dispositivo) {}
