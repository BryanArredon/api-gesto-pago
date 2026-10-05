package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Proyeccion de lectura de la cuenta bancaria. */
public record CuentaDto(
        UUID id,
        UUID clienteId,
        NumeroCuenta numero,
        TipoCuenta tipo,
        String moneda,
        BigDecimal saldo,
        EstatusCuenta estatus,
        Instant creadaEn,
        Instant actualizadaEn) {}
