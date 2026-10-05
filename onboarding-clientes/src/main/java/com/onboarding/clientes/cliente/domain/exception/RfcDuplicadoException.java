package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.Map;

/** Ya existe un cliente registrado con el mismo RFC. */
public class RfcDuplicadoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public RfcDuplicadoException(Rfc rfc) {
        super(CodigoError.RFC_DUPLICADO, "Ya existe un cliente registrado con el RFC indicado", Map.of("rfc", rfc.valor()));
    }
}
