package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Excepcion lanzada cuando las credenciales de inicio de sesion no coinciden. */
public class CredencialesInvalidasException extends ErrorNegocio {

    public CredencialesInvalidasException() {
        super(CodigoError.CREDENCIALES_INVALIDAS, "Credenciales invalidas");
    }
}
