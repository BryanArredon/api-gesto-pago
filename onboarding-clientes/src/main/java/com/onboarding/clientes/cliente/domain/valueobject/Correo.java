package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.DatoInvalidoException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electronico normalizado en minusculas.
 *
 * <p>La normalizacion a minusculas es lo que hace segura la restriccion de unicidad de la base de datos: sin
 * ella, {@code Juan@correo.mx} y {@code juan@correo.mx} seria el mismo usuario para el mundo y dos para el
 * indice unico.
 */
public record Correo(String valor) implements Comparable<Correo> {

    public static final int LONGITUD_MAXIMA = 100;

    /**
     * Expresion regular deliberadamente mas estricta que {@code @Email}: exige dominio con punto y TLD
     * alfabetico, rechaza espacios, comas y punto y coma (vectores classics de inyeccion de cabeceras).
     */
    private static final Pattern ESTRUCTURA =
            Pattern.compile("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]{1,64}@[A-Za-z0-9-]{1,63}(\\.[A-Za-z0-9-]{1,63})*\\.[A-Za-z]{2,24}$");

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El correo electronico es obligatorio", List.of("correo: es obligatorio"));
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > LONGITUD_MAXIMA) {
            throw new DatoInvalidoException(
                    "El correo electronico no puede exceder " + LONGITUD_MAXIMA + " caracteres",
                    List.of("correo: maximo " + LONGITUD_MAXIMA + " caracteres"));
        }
        if (!ESTRUCTURA.matcher(normalizado).matches() || normalizado.contains("..") || normalizado.startsWith(".")
                || normalizado.endsWith("@")) {
            throw new DatoInvalidoException(
                    "El correo electronico no tiene un formato valido", List.of("correo: formato invalido"));
        }
        valor = normalizado;
    }

    public static Correo de(String valor) {
        return new Correo(valor);
    }

    /**
     * @return correo enmascarado para respuestas publicas y logs ({@code ju***@correo.mx})
     */
    public String enmascarado() {
        int arroba = valor.indexOf('@');
        String usuario = valor.substring(0, arroba);
        String visible = usuario.length() <= 2 ? usuario.substring(0, 1) : usuario.substring(0, 2);
        return visible + "***" + valor.substring(arroba);
    }

    @Override
    public int compareTo(Correo otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
