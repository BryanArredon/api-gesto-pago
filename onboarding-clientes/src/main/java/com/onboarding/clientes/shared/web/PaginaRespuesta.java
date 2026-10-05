package com.onboarding.clientes.shared.web;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina de resultados con paginacion por cursor (keyset).
 *
 * @param elementos registros de la pagina actual
 * @param siguienteCursor cursor para la pagina siguiente, {@code null} si no hay mas
 * @param hayMas indica si existe una pagina siguiente
 * @param total elementos existentes en el conjunto completo (opcional)
 * @param <T> tipo del elemento
 */
public record PaginaRespuesta<T>(List<T> elementos, String siguienteCursor, boolean hayMas, Long total) {

    public static <T> PaginaRespuesta<T> de(List<T> elementos, String siguienteCursor, boolean hayMas) {
        return new PaginaRespuesta<>(List.copyOf(elementos), siguienteCursor, hayMas, null);
    }

    public static <T> PaginaRespuesta<T> de(List<T> elementos, String siguienteCursor, boolean hayMas, Long total) {
        return new PaginaRespuesta<>(List.copyOf(elementos), siguienteCursor, hayMas, total);
    }

    public static <T> PaginaRespuesta<T> vacia() {
        return new PaginaRespuesta<>(List.of(), null, false, 0L);
    }

    public static <E, T> PaginaRespuesta<T> construir(
            List<E> lista,
            int limite,
            Function<E, T> mapper,
            Function<E, Cursor> cursorExtractor) {
        if (lista == null || lista.isEmpty()) {
            return vacia();
        }

        boolean hayMas = lista.size() > limite;
        List<E> sublista = hayMas ? lista.subList(0, limite) : lista;

        String siguienteCursor = null;
        if (hayMas && !sublista.isEmpty()) {
            E ultimo = sublista.get(sublista.size() - 1);
            Cursor cursor = cursorExtractor.apply(ultimo);
            if (cursor != null) {
                siguienteCursor = cursor.codificar();
            }
        }

        List<T> transformados = sublista.stream().map(mapper).toList();
        return new PaginaRespuesta<>(transformados, siguienteCursor, hayMas, null);
    }
}
