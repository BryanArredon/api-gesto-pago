package com.onboarding.clientes.shared.error;

import java.util.Map;

/**
 * Error de negocio controlable.
 *
 * <p>Se propaga sin traduccion en la frontera HTTP: {@link ManejadorExcepcionesGlobales} la convierte en
 * un {@code ProblemDetail} (RFC 9457) con el codigo de {@link CodigoError} correspondiente.
 *
 * <p>Las reglas con nombre propio extienden esta clase ({@code CurpDuplicadaException},
 * {@code ClienteNoEncontradoException}, ...); los value objects usan la forma generica pasando el
 * {@link CodigoError} que corresponde.
 *
 * <p>Nunca debe contener datos sensibles en el mensaje: el detalle tecnico se registra en el log interno
 * (con identificador de traza) y el cliente solo recibe el mensaje funcional.
 */
public class ErrorNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient CodigoError codigoError;
    private final transient Map<String, String> detalles;

    public ErrorNegocio(CodigoError codigoError, String mensaje) {
        this(codigoError, mensaje, Map.of());
    }

    public ErrorNegocio(CodigoError codigoError, String mensaje, Map<String, String> detalles) {
        super(mensaje);
        this.codigoError = codigoError;
        this.detalles = Map.copyOf(detalles);
    }

    public CodigoError codigoError() {
        return codigoError;
    }

    /** @return datos adicionales seguros de exponer (por ejemplo, el correo ya enmascarado) */
    public Map<String, String> detalles() {
        return detalles;
    }
}
