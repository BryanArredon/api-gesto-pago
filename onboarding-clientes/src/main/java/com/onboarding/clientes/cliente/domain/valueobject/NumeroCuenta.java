package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.util.regex.Pattern;

/**
 * Numero de cuenta bancaria: 10 digitos, 9 de secuencia y 1 digito verificador Luhn.
 *
 * <p>El digito verificador permite detectar de inmediato los errores de digitacion al introducir el numero
 * (un Simple Typo se detecta en el 90% de los casos) y es la misma tecnica que usan las redes de pago. La
 * asignacion de la secuencia la hace el motor de la base de datos (trigger {@code tg_cuenta_numero}), que
 * garantiza unicidad y concurrencia segura sin depender de la aplicacion.
 */
public record NumeroCuenta(String valor) implements Comparable<NumeroCuenta> {

    public static final int LONGITUD = 10;

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("^[0-9]{10}$");

    public NumeroCuenta {
        if (valor == null || valor.isBlank()) {
            throw new ErrorNegocio(CodigoError.NUMERO_CUENTA_INVALIDO, "El numero de cuenta es obligatorio");
        }
        String normalizado = valor.trim();
        if (!DIEZ_DIGITOS.matcher(normalizado).matches()) {
            throw new ErrorNegocio(
                    CodigoError.NUMERO_CUENTA_INVALIDO, "El numero de cuenta debe tener exactamente 10 digitos");
        }
        if (!digitoVerificadorValido(normalizado)) {
            throw new ErrorNegocio(
                    CodigoError.NUMERO_CUENTA_INVALIDO, "El numero de cuenta no es valido (digito verificador incorrecto)");
        }
        valor = normalizado;
    }

    public static NumeroCuenta de(String valor) {
        return new NumeroCuenta(valor);
    }

    private static boolean digitoVerificadorValido(String numero) {
        int suma = 0;
        for (int i = 0; i < LONGITUD; i++) {
            int digito = Character.digit(numero.charAt(i), 10);
            // Se duplica el digito en las posiciones impares contando desde la derecha.
            boolean duplicar = (LONGITUD - i) % 2 == 0;
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
        }
        return suma % 10 == 0;
    }

    @Override
    public int compareTo(NumeroCuenta otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
