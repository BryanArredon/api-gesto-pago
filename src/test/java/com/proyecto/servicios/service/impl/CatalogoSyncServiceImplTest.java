package com.proyecto.servicios.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoSyncServiceImplTest {

    private static final String CLAVE = "catalogo:productos:activos";

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valores;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RelojMovible reloj = new RelojMovible(Instant.parse("2026-11-22T10:00:00Z"));
    private CatalogoSyncServiceImpl servicio;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valores);
        servicio = new CatalogoSyncServiceImpl(gestoPagoTokenService, jdbcTemplate, redisTemplate,
                objectMapper, "gestor_pagos", CLAVE, Duration.ofMinutes(5), reloj);
    }

    @Test
    void sirveDesdeMemoriaSinVolverARedisMientrasLaCopiaEstaVigente() throws Exception {
        when(valores.get(CLAVE)).thenReturn(objectMapper.writeValueAsString(List.of(producto(1))));

        servicio.obtenerProductosActivos();
        reloj.avanzar(Duration.ofMinutes(4));
        List<CatalogoProductoCache> segunda = servicio.obtenerProductosActivos();

        assertEquals(1, segunda.size());
        verify(valores, times(1)).get(CLAVE);
    }

    @Test
    void recargaDeRedisAlVencerLaCopiaEnMemoria() throws Exception {
        when(valores.get(CLAVE)).thenReturn(objectMapper.writeValueAsString(List.of(producto(1))));

        servicio.obtenerProductosActivos();
        reloj.avanzar(Duration.ofMinutes(6));
        servicio.obtenerProductosActivos();

        verify(valores, times(2)).get(CLAVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void siRedisFallaLeeDeBdYRepueblaRedisComprimidoLegibleDespues() {
        when(valores.get(CLAVE)).thenThrow(new RedisConnectionFailureException("timeout"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(producto(1), producto(2)));

        List<CatalogoProductoCache> desdeBd = servicio.obtenerProductosActivos();

        assertEquals(2, desdeBd.size());
        ArgumentCaptor<String> guardado = ArgumentCaptor.forClass(String.class);
        verify(valores).set(eq(CLAVE), guardado.capture());
        assertTrue(guardado.getValue().startsWith("gz:"));

        // Otra instancia (sin copia en memoria) lee el valor comprimido sin tocar la BD.
        doReturn(guardado.getValue()).when(valores).get(CLAVE);
        CatalogoSyncServiceImpl otraInstancia = new CatalogoSyncServiceImpl(gestoPagoTokenService,
                jdbcTemplate, redisTemplate, objectMapper, "gestor_pagos", CLAVE, Duration.ofMinutes(5), reloj);
        List<CatalogoProductoCache> desdeRedis = otraInstancia.obtenerProductosActivos();

        assertEquals(2, desdeRedis.size());
        assertEquals(new BigDecimal("50.00"), desdeRedis.get(0).getPrecio());
        verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void noRepueblaRedisConUnCatalogoVacio() {
        when(valores.get(CLAVE)).thenReturn(null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        servicio.obtenerProductosActivos();

        verify(valores, never()).set(anyString(), anyString());
    }

    private static CatalogoProductoCache producto(int id) {
        CatalogoProductoCache c = new CatalogoProductoCache();
        c.setIdServicio(100);
        c.setIdProducto(id);
        c.setServicio("Telcel");
        c.setProducto("Recarga " + id);
        c.setIdCatTipoServicio(1);
        c.setTipoFront((short) 1);
        c.setTipoReferencia("a");
        c.setPrecio(new BigDecimal("50.00"));
        return c;
    }

    private static final class RelojMovible extends Clock {
        private Instant ahora;

        RelojMovible(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
