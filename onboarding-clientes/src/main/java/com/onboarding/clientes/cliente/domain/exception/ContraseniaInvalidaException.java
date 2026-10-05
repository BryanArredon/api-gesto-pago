package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Excepcion lanzada cuando la contrasena no cumple la politica de complejidad o longitud. */
public class ContraseniaInvalidaException extends ErrorNegocio {

    public ContraseniaInvalidaException(String mensaje) {
        super(CodigoError.CONTRASENA_INVALIDA, mensaje);
    }
}
