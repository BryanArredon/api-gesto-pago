package com.proyecto.servicios.service.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResilientRateLimiterTest {

    private final RedisRateLimiter redis = mock(RedisRateLimiter.class);
    private final DbRateLimiter db = mock(DbRateLimiter.class);
    private final ResilientRateLimiter limiter = new ResilientRateLimiter(redis, db);

    @Test
    void usaRedisCuandoEstaDisponible() {
        when(redis.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenReturn(true);

        boolean permitido = limiter.tryConsume("ip", "login", 5, Duration.ofSeconds(60));

        assertTrue(permitido);
        verifyNoInteractions(db);
    }

    @Test
    void respetaElResultadoDeRedis() {
        when(redis.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenReturn(false);

        boolean permitido = limiter.tryConsume("ip", "login", 5, Duration.ofSeconds(60));

        assertFalse(permitido);
        verifyNoInteractions(db);
    }

    @Test
    void caeAPostgresCuandoRedisFalla() {
        when(redis.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenThrow(new RuntimeException("redis caido"));
        when(db.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenReturn(true);

        boolean permitido = limiter.tryConsume("ip", "login", 5, Duration.ofSeconds(60));

        assertTrue(permitido);
        verify(db).tryConsume("ip", "login", 5, Duration.ofSeconds(60));
    }

    @Test
    void fallaAbiertoSiRedisYPostgresCaen() {
        when(redis.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenThrow(new RuntimeException("redis caido"));
        when(db.tryConsume("ip", "login", 5, Duration.ofSeconds(60))).thenThrow(new RuntimeException("bd caida"));

        boolean permitido = limiter.tryConsume("ip", "login", 5, Duration.ofSeconds(60));

        assertTrue(permitido);
    }
}