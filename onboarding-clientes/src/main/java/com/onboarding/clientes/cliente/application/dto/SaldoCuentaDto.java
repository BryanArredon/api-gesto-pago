package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import java.math.BigDecimal;

/** DTO que expone el saldo y estatus de una cuenta bancaria. */
public record SaldoCuentaDto(
        NumeroCuenta numeroCuenta,
        String moneda,
        BigDecimal saldo,
        EstatusCuenta estatus) {}
