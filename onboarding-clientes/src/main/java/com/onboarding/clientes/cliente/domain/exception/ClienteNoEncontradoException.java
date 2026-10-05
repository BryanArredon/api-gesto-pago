package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.UUID;

/** No existe un cliente con el identificador o criterio indicado. */
public class ClienteNoEncontradoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public ClienteNoEncontradoException(UUID id) {
        super(CodigoError.CLIENTE_NO_ENCONTRADO, "No existe un cliente con el identificador " + id);
    }

    public ClienteNoEncontradoException(String criterio) {
        super(CodigoError.CLIENTE_NO_ENCONTRADO, "No existe un cliente registrado con " + criterio);
    }

    public static ClienteNoEncontradoException porId(UUID id) {
        return new ClienteNoEncontradoException(id);
    }
}
