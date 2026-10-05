package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyeccion ligera para listados.
 *
 * <p>No carga domicilio ni cuenta: un listado de 50 clientes no debe arrastrar 50 consultas adicionales ni
 * exponer datos que la vista no necesita.
 */
public record ClienteResumen(
        UUID id,
        String nombreCompleto,
        Curp curp,
        Rfc rfc,
        Correo correo,
        LocalDate fechaNacimiento,
        Sexo sexo,
        EstadoCivil estadoCivil,
        String numeroCuenta,
        boolean activo,
        Instant creadoEn) {}
