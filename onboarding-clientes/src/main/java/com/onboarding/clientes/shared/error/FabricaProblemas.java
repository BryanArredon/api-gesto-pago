package com.onboarding.clientes.shared.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ProblemDetail;

/**
 * Construye respuestas de error en formato {@code application/problem+json} (RFC 9457).
 *
 * <p>Formato unico de la API, compartido por el manejo global de excepciones y por los puntos de entrada
 * de Spring Security, de modo que el cliente parsea siempre la misma estructura:
 *
 * <pre>
 * {
 *   "type": "https://errores.onboarding.com/cliente_no_encontrado",
 *   "title": "Cliente no encontrado",
 *   "status": 404,
 *   "detail": "No existe un cliente con el identificador 018f...",
 *   "instance": "/api/v1/clientes/018f...",
 *   "codigo": "cliente_no_encontrado",
 *   "traceId": "6f1c...",
 *   "timestamp": "2026-09-28T18:00:00Z",
 *   "errores": [ { "campo": "curp", "mensaje": "..." } ]
 * }
 * </pre>
 *
 * <p>Nunca se incluye traza de pila, nombres de tablas ni rutas del sistema de archivos.
 */
public final class FabricaProblemas {

    private static final String BASE_TIPOS = "https://errores.onboarding.com/";

    private FabricaProblemas() {}

    public static ProblemDetail crear(CodigoError codigo, String titulo, String detalle, String ruta) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(codigo.estado(), detalle);
        problem.setType(URI.create(BASE_TIPOS + codigo.codigo()));
        problem.setTitle(titulo);
        problem.setInstance(URI.create(ruta));
        problem.setProperty("codigo", codigo.codigo());
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("traceId", TrazaActual.obtener());
        return problem;
    }

    /** Agrega el detalle de campo de una peticion invalida. */
    public static ProblemDetail conErroresDeCampo(ProblemDetail problem, List<ViolacionCampo> errores) {
        if (errores != null && !errores.isEmpty()) {
            problem.setProperty("errores", errores);
        }
        return problem;
    }

    /**
     * Error de validacion de un campo.
     *
     * @param campo ruta del campo (por ejemplo {@code domicilio.codigoPostal})
     * @param mensaje motivo legible
     */
    public record ViolacionCampo(String campo, String mensaje) {}
}
