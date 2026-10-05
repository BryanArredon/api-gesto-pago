package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Operacion rechazada sobre un cliente dado de baja logicamente. */
public class ClienteInactivoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public ClienteInactivoException() {
        super(CodigoError.CLIENTE_INACTIVO, "El cliente se encuentra inactivo y no admite esta operacion");
    }
}
