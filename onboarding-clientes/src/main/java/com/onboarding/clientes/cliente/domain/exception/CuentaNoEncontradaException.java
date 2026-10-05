package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;

/** No existe una cuenta con el numero indicado. */
public class CuentaNoEncontradaException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public CuentaNoEncontradaException(NumeroCuenta numero) {
        super(CodigoError.CUENTA_NO_ENCONTRADA, "No existe una cuenta con el numero " + numero.valor());
    }

    public static CuentaNoEncontradaException porNumero(String numero) {
        return new CuentaNoEncontradaException(NumeroCuenta.de(numero));
    }
}
