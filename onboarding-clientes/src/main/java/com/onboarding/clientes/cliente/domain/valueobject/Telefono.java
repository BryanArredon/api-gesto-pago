package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.DatoInvalidoException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Telefono movil mexicano: exactamente 10 digitos, sin prefijo de pais.
 *
 * <p>Se guarda como texto y no como entero: no se hacen operaciones aritmeticas con telefonos, y un
 * {@code BIGINT} pierde el cero inicial (paisanias que empiezan por 55).
 */
public record Telefono(String valor) implements Comparable<Telefono> {

    public static final int LONGITUD = 10;

    private static final String PREFIJO_PAIS = "52";

    private static final Pattern SOLO_DIGITOS = Pattern.compile("^[0-9]{10}$");

    public Telefono {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El telefono es obligatorio", List.of("telefono: es obligatorio"));
        }
        String normalizado = valor.trim().replaceAll("[\\s\\-().]", "");
        if (normalizado.startsWith("+")) {
            normalizado = normalizado.substring(1);
            if (normalizado.startsWith(PREFIJO_PAIS) && normalizado.length() == LONGITUD + 2) {
                normalizado = normalizado.substring(2);
            }
        }
        if (!SOLO_DIGITOS.matcher(normalizado).matches()) {
            throw new DatoInvalidoException(
                    "El telefono debe contener exactamente 10 digitos (prefijo de pais opcional +52)",
                    List.of("telefono: deben ser exactamente 10 digitos"));
        }
        valor = normalizado;
    }

    public static Telefono de(String valor) {
        return new Telefono(valor);
    }

    @Override
    public int compareTo(Telefono otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
