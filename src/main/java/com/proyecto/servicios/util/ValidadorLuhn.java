package com.proyecto.servicios.util;

import java.util.regex.Pattern;

/**
 * Validador de numeros de cuenta bancaria de 10 digitos con digito verificador Luhn (Mod 10)
 * y utilidades de enmascaramiento seguro para logs y auditoria.
 */
public final class ValidadorLuhn {

    public static final int LONGITUD_CUENTA = 10;
    private static final Pattern DIEZ_DIGITOS = Pattern.compile("^[0-9]{10}$");

    private ValidadorLuhn() {
    }

    public static boolean esNumeroCuentaValido(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return false;
        }
        String normalizado = numeroCuenta.trim();
        if (!DIEZ_DIGITOS.matcher(normalizado).matches()) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < LONGITUD_CUENTA; i++) {
            int digito = Character.digit(normalizado.charAt(i), 10);
            boolean duplicar = (LONGITUD_CUENTA - i) % 2 == 0;
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

    public static String enmascararCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return "******";
        }
        String limpio = numeroCuenta.trim();
        if (limpio.length() <= 4) {
            return "******";
        }
        return "******" + limpio.substring(limpio.length() - 4);
    }
}
