package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyeccion de lectura completa del cliente.
 *
 * <p>Es un record inmutable y no una entidad JPA: la capa de persistencia nunca sale de
 * {@code infrastructure} (regla verificada por {@code ArquitecturaTest}).
 */
public record ClienteDetalle(
        UUID id,
        String nombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        Curp curp,
        Rfc rfc,
        Sexo sexo,
        String nacionalidad,
        EstadoCivil estadoCivil,
        Correo correo,
        Telefono telefonoMovil,
        Telefono telefonoAlterno,
        String ocupacion,
        String empresa,
        BigDecimal ingresoMensual,
        String monedaIngreso,
        boolean activo,
        Instant fechaBaja,
        Instant creadoEn,
        Instant actualizadoEn,
        DomicilioDto domicilio,
        CuentaDto cuenta,
        UsuarioDto usuario) {}
