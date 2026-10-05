package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.math.BigDecimal;

/** El saldo inicial solicitado no cumple la regla de negocio (debe ser mayor o igual a cero). */
public class SaldoInvalidoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public SaldoInvalidoException(BigDecimal saldo) {
        super(CodigoError.SALDO_INVALIDO, "El saldo inicial no puede ser negativo: " + saldo);
    }
}
