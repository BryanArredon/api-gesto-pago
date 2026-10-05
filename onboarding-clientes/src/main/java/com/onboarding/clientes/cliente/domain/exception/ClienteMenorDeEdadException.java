package com.onboarding.clientes.cliente.domain.exception;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.time.LocalDate;
import java.time.Period;

/** El cliente no cumple la edad minima requerida (18 anos) para abrir una cuenta. */
public class ClienteMenorDeEdadException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    public ClienteMenorDeEdadException(LocalDate fechaNacimiento, LocalDate hoy, int edadMinima) {
        super(
                CodigoError.CLIENTE_MENOR_DE_EDAD,
                "El cliente debe ser mayor de edad (" + edadMinima + " anos) para abrir una cuenta. Fecha de nacimiento: "
                        + fechaNacimiento,
                java.util.Map.of("edadMinima", String.valueOf(edadMinima)));
    }

    /** @return edad cumplida a la fecha indicada */
    public static int edadEn(LocalDate fechaNacimiento, LocalDate hoy) {
        return Period.between(fechaNacimiento, hoy).getYears();
    }
}
