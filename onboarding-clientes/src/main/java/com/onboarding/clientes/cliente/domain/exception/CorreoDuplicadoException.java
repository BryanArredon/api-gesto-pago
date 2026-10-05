package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.Map;

/** Ya existe un cliente registrado con el mismo correo electronico. */
public class CorreoDuplicadoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public CorreoDuplicadoException(Correo correo) {
        super(
                CodigoError.CORREO_DUPLICADO,
                "Ya existe un cliente registrado con ese correo electronico",
                Map.of("correo", correo.enmascarado()));
    }
}
