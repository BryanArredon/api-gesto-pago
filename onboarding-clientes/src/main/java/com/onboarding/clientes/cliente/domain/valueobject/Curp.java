package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Clave Unica de Registro de Poblacion: identificador oficial de la persona fisica en Mexico.
 *
 * <p>Estructura de 18 caracteres (RENAPO):
 * 1-4: iniciales y letras
 * 5-10: fecha de nacimiento AAMMDD
 * 11: sexo (H/M)
 * 12-13: entidad federativa (2 letras)
 * 14-16: consonantes internas
 * 17: diferenciador de siglo (0-9 o A-Z)
 * 18: digito verificador
 */
public record Curp(String valor) implements Comparable<Curp> {

    public static final int LONGITUD = 18;

    private static final Pattern ESTRUCTURA =
            Pattern.compile("^[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9A-Z]{2}$");

    public Curp {
        if (valor == null || valor.isBlank()) {
            throw new CurpInvalidaException("La CURP es obligatoria");
        }
        String normalizada = valor.trim().toUpperCase(Locale.ROOT);
        if (normalizada.length() != LONGITUD) {
            throw new CurpInvalidaException(
                    "La CURP debe tener exactamente " + LONGITUD + " caracteres: " + normalizada.length() + " recibidos");
        }
        if (!ESTRUCTURA.matcher(normalizada).matches()) {
            throw new CurpInvalidaException("La CURP no cumple el formato oficial de 18 caracteres");
        }
        if (!fechaEmbebidaValida(normalizada)) {
            throw new CurpInvalidaException("La CURP contiene una fecha de nacimiento inexistente");
        }
        valor = normalizada;
    }

    public static Curp de(String valor) {
        return new Curp(valor);
    }

    /**
     * Regla de negocio: la CURP es la identidad oficial y su fecha embebida debe coincidir con la fecha de
     * nacimiento declarada (AAMMDD).
     *
     * @param fechaNacimiento fecha de nacimiento declarada por el cliente
     */
    public void exigeCoincidirCon(LocalDate fechaNacimiento) {
        int anio = Integer.parseInt(valor.substring(4, 6));
        int mes = Integer.parseInt(valor.substring(6, 8));
        int dia = Integer.parseInt(valor.substring(8, 10));

        if (fechaNacimiento.getYear() % 100 != anio
                || fechaNacimiento.getMonthValue() != mes
                || fechaNacimiento.getDayOfMonth() != dia) {
            throw new CurpInvalidaException("La fecha de nacimiento de la CURP no coincide con la fecha de nacimiento declarada");
        }
    }

    public Character sexoCodificado() {
        char caracter = valor.charAt(10);
        return (caracter == 'H' || caracter == 'M') ? caracter : null;
    }

    private static boolean fechaEmbebidaValida(String curp) {
        try {
            int anio = Integer.parseInt(curp.substring(4, 6));
            int century = anio <= 29 ? 2000 : 1900;
            int mes = Integer.parseInt(curp.substring(6, 8));
            int dia = Integer.parseInt(curp.substring(8, 10));
            LocalDate.of(century + anio, mes, dia);
            return true;
        } catch (DateTimeException | NumberFormatException excepcion) {
            return false;
        }
    }

    @Override
    public int compareTo(Curp otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }

    /** Error de dominio especifico de CURP. */
    public static final class CurpInvalidaException extends ErrorNegocio {

        private static final long serialVersionUID = 1L;

        public CurpInvalidaException(String mensaje) {
            super(CodigoError.CURP_INVALIDA, mensaje);
        }
    }
}
