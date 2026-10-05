package com.onboarding.clientes.shared.seguridad;

import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.error.DatoInvalidoException;
import java.util.ArrayList;
import java.util.List;

/**
 * Politica de credenciales del cliente.
 *
 * <p>Contrasena: minimo 8 caracteres, al menos una mayuscula, una minuscula, un digito y un caracter
 * especial. Es el minimo razonable para un acceso que mueve dinero; el coste de exigir mas (frases de paso)
 * se mide en usabilidad y en la tasa de recuperacion de cuentas.
 *
 * <p>PIN: 4 a 6 digitos. Es deliberadamente mas corto porque se teclea en un movil y sirve como segundo
 * factor local, no como credencial unica: el PIN solo es aceptable si el dispositivo esta enrolado con una
 * llave biometrica o la cuenta tiene un limite de intentos.
 */
public final class PoliticaCredenciales {

    private PoliticaCredenciales() {}

    /**
     * @param contrasenia contrasena en claro
     * @param longitudMinima longitud minima exigida por configuracion
     * @throws DatoInvalidoException si no cumple la politica
     */
    public static void validarContrasenia(String contrasenia, int longitudMinima) {
        List<String> problemas = new ArrayList<>();
        if (contrasenia == null || contrasenia.isEmpty()) {
            throw new DatoInvalidoException("La contrasena es obligatoria", List.of("contrasenia: es obligatoria"));
        }
        if (contrasenia.length() < longitudMinima) {
            problemas.add("contrasenia: debe tener al menos " + longitudMinima + " caracteres");
        }
        if (contrasenia.length() > 72) {
            // Limite de BCrypt: los bytes adicionales se truncan silenciosamente, lo que es una debilidad.
            problemas.add("contrasena: no puede exceder 72 caracteres");
        }
        if (!contrasenia.chars().anyMatch(Character::isUpperCase)) {
            problemas.add("contrasena: debe incluir al menos una letra mayuscula");
        }
        if (!contrasenia.chars().anyMatch(Character::isLowerCase)) {
            problemas.add("contrasena: debe incluir al menos una letra minuscula");
        }
        if (!contrasenia.chars().anyMatch(Character::isDigit)) {
            problemas.add("contrasena: debe incluir al menos un numero");
        }
        if (contrasenia.chars().noneMatch(PoliticaCredenciales::esEspecial)) {
            problemas.add("contrasena: debe incluir al menos un caracter especial");
        }
        if (!problemas.isEmpty()) {
            throw new DatoInvalidoException("La contrasena no cumple la politica de seguridad", problemas);
        }
    }

    /**
     * @param pin PIN en claro
     * @param minimo longitud minima configurada
     * @param maximo longitud maxima configurada
     * @throws DatoInvalidoException con el codigo {@code contrasena_invalida}
     */
    public static void validarPin(String pin, int minimo, int maximo) {
        if (pin == null || pin.isBlank()) {
            throw new DatoInvalidoException("El PIN es obligatorio", List.of("pin: es obligatorio"));
        }
        String normalizado = pin.trim();
        if (normalizado.length() < minimo || normalizado.length() > maximo) {
            throw new DatoInvalidoException(
                    "El PIN debe tener entre " + minimo + " y " + maximo + " digitos",
                    List.of("pin: debe tener entre " + minimo + " y " + maximo + " digitos"));
        }
        if (!normalizado.chars().allMatch(Character::isDigit)) {
            throw new DatoInvalidoException(
                    "El PIN debe contener solo digitos", List.of("pin: solo admite digitos"));
        }
    }

    /** Regla adicional: el PIN nunca puede ser igual a la contrasena. */
    public static void exigePinDistintoDeContrasena(String pin, String contrasenia) {
        if (pin != null && pin.equals(contrasenia)) {
            throw new DatoInvalidoException(
                    "El PIN no puede ser igual a la contrasena", List.of("pin: debe ser distinto de la contrasena"));
        }
    }

    private static boolean esEspecial(int caracter) {
        return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
    }

    /** Atajo con la configuracion por defecto (usado por comandos simples). */
    public static void validarContrasenia(String contrasenia) {
        validarContrasenia(contrasenia, 8);
    }

    /** Valida con los limites configurados en la aplicacion. */
    public static void validarContrasenia(String contrasenia, PropiedadesOnboarding.Seguridad seguridad) {
        validarContrasenia(contrasenia, seguridad.longitudMinimaContrasenia());
    }
}
