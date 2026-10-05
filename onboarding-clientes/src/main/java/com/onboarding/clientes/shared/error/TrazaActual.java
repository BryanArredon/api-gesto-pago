package com.onboarding.clientes.shared.error;

import org.slf4j.MDC;

/**
 * Acceso al identificador de traza desde cualquier capa.
 *
 * <p>El identificador lo inyecta el filtro de traza (Micrometer Tracing) en el MDC y se devuelve en el
 * cuerpo del error para que soporte pueda correlacionar la peticion con los logs sin ver datos del
 * cliente.
 */
public final class TrazaActual {

    /** Clave del MDC gestionada por Micrometer Tracing. */
    public static final String CLAVE_TRACE_ID = "traceId";

    private TrazaActual() {}

    /** @return identificador de traza actual o {@code "desconocido"} si la peticion no tiene traza */
    public static String obtener() {
        String traza = MDC.get(CLAVE_TRACE_ID);
        if (traza == null || traza.isBlank()) {
            String identificadorPeticion = MDC.get("requestId");
            return identificadorPeticion == null || identificadorPeticion.isBlank() ? "desconocido" : identificadorPeticion;
        }
        return traza;
    }
}
