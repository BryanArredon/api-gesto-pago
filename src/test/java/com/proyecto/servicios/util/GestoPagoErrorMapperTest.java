package com.proyecto.servicios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestoPagoErrorMapperTest {

    @Test
    void mapeaCodigoReferenciaInvalida() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("02", "Referencia invalida");
        assertEquals("02", err.codigo());
        assertTrue(err.explicacionAmigable().contains("referencia"));
    }

    @Test
    void mapeaCodigoCarrierNoResponde() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("03", "Por el momento el carrier no responde");
        assertEquals("03", err.codigo());
        assertTrue(err.explicacionAmigable().contains("carrier"));
    }

    @Test
    void mapeaCodigoSaldoInsuficienteDistribuidor() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("04", "Distribuidor sin saldo");
        assertEquals("04", err.codigo());
        assertTrue(err.explicacionAmigable().contains("saldo"));
    }

    @Test
    void mapeaCodigoDispositivoNoRegistrado() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("51", "Dispositivo no registrado");
        assertEquals("51", err.codigo());
        assertTrue(err.explicacionAmigable().contains("Dispositivo"));
    }

    @Test
    void mapeaCodigoTransaccionNoAplicada() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("70", "La transaccion no fue aplicada");
        assertEquals("70", err.codigo());
        assertTrue(err.explicacionAmigable().contains("no fue aplicada"));
    }

    @Test
    void mapeaCodigoTimeout() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear("82", "Timeout");
        assertEquals("82", err.codigo());
        assertTrue(err.explicacionAmigable().contains("Timeout") || err.explicacionAmigable().contains("espera"));
    }

    @Test
    void infiereCodigoYExplicacionDesdeTextoCuandoCodigoEsNulo() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear(null, "Carrier no responde");
        assertEquals("03", err.codigo());
        assertTrue(err.explicacionAmigable().contains("carrier"));
    }

    @Test
    void manejaValoresNulosYDesconocidos() {
        GestoPagoErrorMapper.ErrorGestoPago err = GestoPagoErrorMapper.mapear(null, null);
        assertNotNull(err.codigo());
        assertNotNull(err.explicacionAmigable());
    }
}

