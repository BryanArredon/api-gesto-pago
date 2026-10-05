package com.onboarding.clientes.shared.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Cursor opaco de paginacion por clave (keyset pagination).
 *
 * <p>Codifica la posicion exacta de la ultima fila leida como {@code marcaTiempo|uuid} en Base64 URL-safe.
 * El cliente lo devuelve tal cual y el servidor lo decodifica para construir el {@code WHERE} con indices
 * compound:
 *
 * <pre>
 *   WHERE (created_at, id) &lt; (:marca, :id)
 * </pre>
 *
 * <p>Ventajas frente a {@code LIMIT/OFFSET}: coste constante en cualquier pagina, resultados estables
 * aunque se inserten filas nuevas y ninguna dependencia de la profundidad del conjunto.
 */
public record Cursor(Instant marcaTiempo, UUID id) {

    public String codificar() {
        String crudo = marcaTiempo.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(crudo.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param cursor cursor recibido del cliente, puede ser {@code null}
     * @return cursor decodificado o {@code null} si la peticion es la primera
     * @throws CursorInvalidoException si el cursor no tiene el formato esperado
     */
    public static Cursor decodificar(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String crudo = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separador = crudo.indexOf('|');
            if (separador <= 0) {
                throw new CursorInvalidoException(cursor);
            }
            return new Cursor(Instant.parse(crudo.substring(0, separador)), UUID.fromString(crudo.substring(separador + 1)));
        } catch (IllegalArgumentException excepcion) {
            throw new CursorInvalidoException(cursor);
        }
    }

    /** Error de cliente: el cursor recibido no es un cursor emitido por esta API. */
    public static class CursorInvalidoException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        CursorInvalidoException(String cursor) {
            super("El cursor proporcionado no es valido: " + cursor);
        }
    }
}
