package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/** Excepcion lanzada cuando el usuario esta temporalmente bloqueado por exceso de intentos fallidos. */
public class UsuarioBloqueadoException extends ErrorNegocio {

    public UsuarioBloqueadoException(String mensaje) {
        super(CodigoError.USUARIO_BLOQUEADO, mensaje);
    }
}
