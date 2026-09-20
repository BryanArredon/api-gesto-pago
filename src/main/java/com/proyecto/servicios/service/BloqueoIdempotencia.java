package com.proyecto.servicios.service;

import com.proyecto.servicios.model.idempotencia.ClaveIdempotencia;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BloqueoIdempotencia {

    private static final int HASH_SEMILLA = 0x811c9dc5;
    private static final int HASH_PRIME = 0x01000193;

    private final JdbcTemplate jdbcTemplate;

    public BloqueoIdempotencia(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adquiere un lock transaccional por clave de idempotencia. Solo una
     * peticion simultanea con la misma clave puede avanzar; las demas
     * esperan y luego releen la fila ya persistida (respuestas duplicadas).
     */
    public void adquirir(ClaveIdempotencia clave) {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + fnv1a(clave) + ")");
    }

    private long fnv1a(ClaveIdempotencia clave) {
        int hash = HASH_SEMILLA;
        String texto = clave.usuarioId() + ":" + clave.idempotencyKey();
        for (int i = 0; i < texto.length(); i++) {
            hash ^= texto.charAt(i);
            hash *= HASH_PRIME;
        }
        return Integer.toUnsignedLong(hash);
    }
}