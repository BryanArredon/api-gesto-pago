package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Ya existe un usuario asociado a otro cliente con el mismo correo. */
public class UsuarioYaRegistradoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public UsuarioYaRegistradoException() {
        super(CodigoError.CLIENTE_YA_REGISTRADO, "El cliente ya tiene un usuario de acceso asociado");
    }
}
