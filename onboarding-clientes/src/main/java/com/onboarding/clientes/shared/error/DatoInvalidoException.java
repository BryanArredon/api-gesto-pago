package com.onboarding.clientes.shared.error;

import java.util.List;

/**
 * Error de validacion de un valor concreto del dominio (no de la forma de la peticion).
 *
 * <p>Ejemplos: una CURP con formato invalido, un RFC cuya fecha embebida no coincide con la fecha de
 * nacimiento, un saldo negativo. La validacion sintactica de la peticion (longitudes, tipos, campos
 * obligatorios) se resuelve antes con Jakarta Bean Validation en el borde HTTP.
 */
public class DatoInvalidoException extends ErrorNegocio {

    private static final long serialVersionUID = 1L;

    private final transient List<String> problemas;

    public DatoInvalidoException(String mensaje, List<String> problemas) {
        super(CodigoError.DATO_INVALIDO, mensaje);
        this.problemas = List.copyOf(problemas);
    }

    public List<String> problemas() {
        return problemas;
    }
}
