package com.onboarding.clientes.shared.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementacion de {@link GeneradorHash} con BCrypt.
 *
 * <p>Coste 12: ~250 ms por verificacion en hardware de servidor actual. Es deliberadamente caro porque el
 * objetivo es frenar el ataque offline por fuerza bruta sobre una base filtrada; el coste se compensa con
 * que solo se verifica en el login y con limites de intentos por usuario.
 */
@Component
public class GeneradorHashBcrypt implements GeneradorHash {

    private static final int COSTE = 12;

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(COSTE);

    @Override
    public String hash(String textoPlano) {
        if (textoPlano == null || textoPlano.isEmpty()) {
            throw new IllegalArgumentException("No se puede cifrar una credencial vacia");
        }
        return codificador.encode(textoPlano);
    }

    @Override
    public boolean coincide(String textoPlano, String hash) {
        if (textoPlano == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return codificador.matches(textoPlano, hash);
        } catch (IllegalArgumentException excepcion) {
            // Hash con formato desconocido (por ejemplo, importado de otro algoritmo): no coincide.
            return false;
        }
    }

    @Override
    public boolean necesitaRehash(String hash) {
        return hash != null && codificador.upgradeEncoding(hash);
    }
}
