package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.DatoInvalidoException;
import java.util.List;
import java.util.regex.Pattern;

/** Codigo postal mexicano: exactamente 5 digitos. */
public record CodigoPostal(String valor) implements Comparable<CodigoPostal> {

    public static final int LONGITUD = 5;

    private static final Pattern CINCO_DIGITOS = Pattern.compile("^[0-9]{5}$");

    public CodigoPostal {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El codigo postal es obligatorio", List.of("codigoPostal: es obligatorio"));
        }
        String normalizado = valor.trim();
        if (!CINCO_DIGITOS.matcher(normalizado).matches()) {
            throw new DatoInvalidoException(
                    "El codigo postal debe contener exactamente 5 digitos", List.of("codigoPostal: deben ser 5 digitos"));
        }
        valor = normalizado;
    }

    public static CodigoPostal de(String valor) {
        return new CodigoPostal(valor);
    }

    @Override
    public int compareTo(CodigoPostal otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
