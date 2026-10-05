package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Registro Federal de Contribuyentes de persona fisica: 13 caracteres, 4 letras + fecha AAMMDD + homoclave.
 *
 * <p>La century del anio no viaja en el RFC, se deduce del contexto: {@code 00-29} corresponde a 2000-2029 y
 * {@code 30-99} a 1930-1999. Esa inferencia se usa para validar que la fecha embebida exista y coincida con
 * la fecha de nacimiento declarada, que es la unica forma de deterner la equivalencia de una persona fisica.
 */
public record Rfc(String valor) implements Comparable<Rfc> {

    public static final int LONGITUD_PERSONA_FISICA = 13;

    private static final Pattern ESTRUCTURA = Pattern.compile("^[A-ZÑ&]{4}[0-9]{6}[0-9A-Z]{3}$");

    public Rfc {
        if (valor == null || valor.isBlank()) {
            throw new RfcInvalidoException("El RFC es obligatorio");
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() != 12 && normalizado.length() != LONGITUD_PERSONA_FISICA) {
            throw new RfcInvalidoException("El RFC debe tener 13 caracteres (persona fisica) o 12 (persona moral)");
        }
        if (!ESTRUCTURA.matcher(normalizado).matches()) {
            throw new RfcInvalidoException("El RFC no cumple el formato oficial");
        }
        if (normalizado.length() == LONGITUD_PERSONA_FISICA && !fechaEmbebidaValida(normalizado)) {
            throw new RfcInvalidoException("El RFC contiene una fecha de nacimiento inexistente");
        }
        valor = normalizado;
    }

    public static Rfc de(String valor) {
        return new Rfc(valor);
    }

    public boolean esPersonaFisica() {
        return valor.length() == LONGITUD_PERSONA_FISICA;
    }

    /**
     * Regla de negocio: la fecha del RFC debe corresponder a la fecha de nacimiento registrada.
     *
     * @param fechaNacimiento fecha de nacimiento declarada
     */
    public void exigeCoincidirCon(LocalDate fechaNacimiento) {
        if (!esPersonaFisica()) {
            return;
        }
        int anio = Integer.parseInt(valor.substring(4, 6));
        int century = anio <= 29 ? 2000 : 1900;
        LocalDate fechaRfc = LocalDate.of(century + anio, Integer.parseInt(valor.substring(6, 8)),
                Integer.parseInt(valor.substring(8, 10)));
        if (!fechaRfc.equals(fechaNacimiento)) {
            throw new RfcInvalidoException("La fecha de nacimiento del RFC no coincide con la fecha de nacimiento declarada");
        }
    }

    private static boolean fechaEmbebidaValida(String rfc) {
        int anio = Integer.parseInt(rfc.substring(4, 6));
        int century = anio <= 29 ? 2000 : 1900;
        try {
            LocalDate.of(
                    century + anio,
                    Integer.parseInt(rfc.substring(6, 8)),
                    Integer.parseInt(rfc.substring(8, 10)));
            return true;
        } catch (DateTimeException | NumberFormatException excepcion) {
            return false;
        }
    }

    @Override
    public int compareTo(Rfc otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }

    /** Error de dominio especifico de RFC. */
    public static final class RfcInvalidoException extends ErrorNegocio {

        private static final long serialVersionUID = 1L;

        public RfcInvalidoException(String mensaje) {
            super(CodigoError.RFC_INVALIDO, mensaje);
        }
    }
}
