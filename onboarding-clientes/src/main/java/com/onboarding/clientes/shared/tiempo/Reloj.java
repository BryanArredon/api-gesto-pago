package com.onboarding.clientes.shared.tiempo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Fuente de tiempo del dominio.
 *
 * <p>Registrarla como puerto permite congelar el tiempo en las pruebas y garantiza que las reglas de
 * negocio (mayoria de edad, expiracion de tokens, vigencias) no dependan de {@code Instant.now()} disperso
 * por el codigo.
 */
public interface Reloj {

    /** @return instante actual en UTC */
    Instant ahora();

    /**
     * @param zona zona de referencia
     * @return fecha local actual
     */
    default LocalDate hoy(ZoneId zona) {
        return ahora().atZone(zona).toLocalDate();
    }
}
