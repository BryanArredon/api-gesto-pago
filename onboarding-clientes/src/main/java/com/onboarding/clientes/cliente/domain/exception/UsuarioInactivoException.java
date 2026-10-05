package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Excepcion lanzada cuando un usuario inactivo intenta autenticarse o realizar operaciones. */
public class UsuarioInactivoException extends ErrorNegocio {

    public UsuarioInactivoException(String correo) {
        super(CodigoError.USUARIO_INACTIVO, "El usuario asociado a '" + correo + "' se encuentra inactivo");
    }
}
