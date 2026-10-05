package com.onboarding.clientes.shared.tiempo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Implementacion de {@link Reloj} delegando en el reloj del sistema. */
@Component
public class RelojSistema implements Reloj {

    private final Clock reloj;

    public RelojSistema(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public Instant ahora() {
        return reloj.instant();
    }

    @Override
    public LocalDate hoy(ZoneId zona) {
        return LocalDate.now(reloj.withZone(zona));
    }
}
