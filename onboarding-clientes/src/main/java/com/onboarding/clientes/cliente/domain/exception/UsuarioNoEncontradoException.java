package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.UUID;

/** No existe un usuario de acceso con el identificador indicado. */
public class UsuarioNoEncontradoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public UsuarioNoEncontradoException(UUID id) {
        super(CodigoError.USUARIO_NO_ENCONTRADO, "No existe un usuario con el identificador " + id);
    }

    public static UsuarioNoEncontradoException porId(UUID id) {
        return new UsuarioNoEncontradoException(id);
    }
}
