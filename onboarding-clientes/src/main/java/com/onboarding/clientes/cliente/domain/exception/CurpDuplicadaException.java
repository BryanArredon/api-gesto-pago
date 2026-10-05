package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.Map;

/** Ya existe un cliente registrado con la misma CURP. */
public class CurpDuplicadaException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public CurpDuplicadaException(Curp curp) {
        super(CodigoError.CURP_DUPLICADA, "Ya existe un cliente registrado con la CURP indicada", Map.of("curp", curp.valor()));
    }
}
