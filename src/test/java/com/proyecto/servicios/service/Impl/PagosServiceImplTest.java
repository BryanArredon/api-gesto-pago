package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.EventoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.pago.PagoRequestDto;
import com.proyecto.servicios.model.pago.TransaccionDto;
import com.proyecto.servicios.model.pago.VerificarReferenciaRequest;
import com.proyecto.servicios.model.pago.VerificarReferenciaResponse;
import com.proyecto.servicios.repositorys.pago.EventoTransaccionRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionRepository;
import com.proyecto.servicios.service.BloqueoIdempotencia;
import com.proyecto.servicios.service.CatalogoConsulta;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagosServiceImplTest {

    private static final String XML_SENDTX_EXITO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <ID_TX>1975170643</ID_TX>
                <NUM_AUTORIZACION>100048015</NUM_AUTORIZACION>
                <SALDO>30484.32</SALDO>
                <COMISION>6.0</COMISION>
                <FECHA>12/Mar/2024 15:10:48</FECHA>
                <MONTO>701.99</MONTO>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                    <REFERENCIA>25720381</REFERENCIA>
                </MENSAJE>
            </RESPONSE>
            """;
    private static final String XML_CONFIRM_EXITO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>06</CODIGO>
                    <TEXTO>La transaccion fue aplicada exitosamente. Autorizacion [100024988] idTicket[291437188]</TEXTO>
                    <ID_TX>291437188</ID_TX>
                </MENSAJE>
            </RESPONSE>
            """;
    private static final String XML_CONFIRM_NO_APLICADA = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <NUM_AUTORIZACION>0</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>70</CODIGO>
                    <TEXTO>La transaccion no fue aplicada</TEXTO>
                    <ID_TX>-1</ID_TX>
                </MENSAJE>
            </RESPONSE>
            """;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private GestoPagoTxClient gestoPagoTxClient;
    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private EventoTransaccionRepository eventoTransaccionRepository;
    @Mock
    private CatalogoConsulta catalogoConsulta;
    @Mock
    private BloqueoIdempotencia bloqueoIdempotencia;

    private PagosServiceImpl service;
    private GestoPagoToken token;

    @BeforeEach
    void setUp() {
        service = new PagosServiceImpl(gestoPagoTokenService, gestoPagoTxClient,
                transaccionRepository, eventoTransaccionRepository,
                catalogoConsulta, bloqueoIdempotencia, 1, "DISP01", "083", 62);
        token = new GestoPagoToken();
        token.setToken("token-de-prueba");
        lenient().when(gestoPagoTokenService.obtenerTokenActivo(1, "DISP01"))
                .thenReturn(Optional.of(token));
    }

    private CatalogoProductoCache recarga() {
        CatalogoProductoCache p = new CatalogoProductoCache();
        p.setIdServicio(76);
        p.setIdProducto(205);
        p.setServicio("Telcel");
        p.setProducto("Recarga 100");
        p.setIdCatTipoServicio(2);
        p.setTipoFront((short) 1);
        p.setTipoReferencia("a");
        p.setPrecio(new BigDecimal("100.00"));
        return p;
    }

    private CatalogoProductoCache servicio() {
        CatalogoProductoCache p = new CatalogoProductoCache();
        p.setIdServicio(108);
        p.setIdProducto(272);
        p.setServicio("Agua");
        p.setProducto("Pago de recibo");
        p.setIdCatTipoServicio(3);
        p.setTipoFront((short) 2);
        p.setTipoReferencia("bc");
        return p;
    }

    private CatalogoProductoCache verificable() {
        CatalogoProductoCache p = new CatalogoProductoCache();
        p.setIdServicio(1125);
        p.setIdProducto(597);
        p.setServicio("Codigo promocional");
        p.setProducto("Codigo promocional");
        p.setIdCatTipoServicio(6);
        p.setTipoFront((short) 4);
        return p;
    }

    private VerificarReferenciaRequest request(Integer idServicio, Integer idProducto, String referencia) {
        VerificarReferenciaRequest r = new VerificarReferenciaRequest();
        r.setIdServicio(idServicio);
        r.setIdProducto(idProducto);
        r.setReferencia(referencia);
        return r;
    }

    private PagoRequestDto pago(CatalogoProductoCache producto, String referencia, BigDecimal monto) {
        PagoRequestDto r = new PagoRequestDto();
        r.setIdServicio(producto.getIdServicio());
        r.setIdProducto(producto.getIdProducto());
        r.setReferencia(referencia);
        r.setMonto(monto);
        r.setIdempotencyKey("clave-idem-123");
        return r;
    }

    @Test
    void verificarReferenciaValidaDevuelveSaldo() {
        when(catalogoConsulta.buscarActivo(1125, 597)).thenReturn(Optional.of(verificable()));
        when(gestoPagoTxClient.verifyReference(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <RESPONSE>
                            <MENSAJE>
                                <CODIGO>01</CODIGO>
                                <TEXTO>Referencia Valida</TEXTO>
                                <SALDO>1034.5</SALDO>
                            </MENSAJE>
                        </RESPONSE>
                        """);

        VerificarReferenciaResponse respuesta = service.verificarReferencia(request(1125, 597, "GP0050OWE"));

        assertTrue(respuesta.getValida());
        assertEquals(0, respuesta.getCodigo());
        assertEquals("1034.5", respuesta.getMonto());
    }

    @Test
    void verificarReferenciaInvalidaDevuelveMensaje() {
        when(catalogoConsulta.buscarActivo(1125, 597)).thenReturn(Optional.of(verificable()));
        when(gestoPagoTxClient.verifyReference(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <RESPONSE>
                            <MENSAJE>
                                <CODIGO>02</CODIGO>
                                <TEXTO>The code is not valid</TEXTO>
                            </MENSAJE>
                        </RESPONSE>
                        """);

        VerificarReferenciaResponse respuesta = service.verificarReferencia(request(1125, 597, "INVALIDO"));

        assertFalse(respuesta.getValida());
        assertTrue(respuesta.getMensaje().contains("not valid"));
    }

    @Test
    void verificarReferenciaDeProductoNoVerificableEsRechazada() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));

        ApiException error = assertThrows(ApiException.class,
                () -> service.verificarReferencia(request(76, 205, "5577777777")));
        assertEquals("PAGO-002", error.getCode());
        verify(gestoPagoTxClient, never()).verifyReference(anyString(), any());
    }

    @Test
    void crearRecargaExitosaMarcaAprobada() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> {
            Transaccion t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(10L);
                t.setCreatedAt(Instant.now());
                t.setUpdatedAt(Instant.now());
            }
            return t;
        });
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_EXITO);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("APROBADA", dto.getEstado());
        assertEquals("100048015", dto.getNumeroAutorizacion());
        assertEquals("1975170643", dto.getIdTx());
        assertEquals("701.99", dto.getMonto());
        assertEquals("6.0", dto.getComision());
        assertEquals("clave-idem-123", dto.getUpc());

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(gestoPagoTxClient).sendTx(anyString(), captor.capture());
        Map<String, String> body = captor.getValue();
        assertEquals("76", body.get("idServicio"));
        assertEquals("205", body.get("idProducto"));
        assertEquals("5577777777", body.get("telefono"));
        assertEquals("clave-idem-123", body.get("upc"));
        assertEquals("083", body.get("unidad"));
        assertNotNull(body.get("horaLocal"));
        assertFalse(body.containsKey("montoPago"));
        assertFalse(body.containsKey("referencia"));
    }

    @Test
    void crearServicioEnviaReferenciaMontoYTelefonoFijo() {
        when(catalogoConsulta.buscarActivo(108, 272)).thenReturn(Optional.of(servicio()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> {
            Transaccion t = inv.getArgument(0);
            t.setId(11L);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_EXITO);

        service.crearTransaccion(1L, pago(servicio(), "5610440665", new BigDecimal("601.99")));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(gestoPagoTxClient).sendTx(anyString(), captor.capture());
        Map<String, String> body = captor.getValue();
        assertEquals("1111111111", body.get("telefono"));
        assertEquals("5610440665", body.get("referencia"));
        assertEquals("601.99", body.get("montoPago"));
    }

    @Test
    void crearTransaccionDuplicadaDevuelveLaExistenteSinLlamarAlProveedor() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion existente = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-otra");
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.of(existente));

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("EN_PROCESO", dto.getEstado());
        verify(gestoPagoTxClient, never()).sendTx(anyString(), any());
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void crearReintentaTransaccionFallidaReutilizandoLaFila() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion fallida = transaccion(76, 205, EstadoTransaccion.FALLIDA, "clave-idem-123");
        fallida.setErrorMensaje("Saldo insuficiente");
        fallida.setUpc("clave-idem-123");
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.of(fallida));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_EXITO);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("APROBADA", dto.getEstado());
        assertEquals("clave-idem-123", dto.getUpc());
    }

    @Test
    void crearConProductoNoActivoEsRechazado() {
        when(catalogoConsulta.buscarActivo(999, 1)).thenReturn(Optional.empty());
        CatalogoProductoCache p = new CatalogoProductoCache();
        p.setIdServicio(999);
        p.setIdProducto(1);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pago(p, "12345", new BigDecimal("50.00"))));
        assertEquals("PAGO-003", error.getCode());
    }

    @Test
    void crearServicioSinMontoEsRechazado() {
        when(catalogoConsulta.buscarActivo(108, 272)).thenReturn(Optional.of(servicio()));

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pago(servicio(), "5610440665", null)));
        assertEquals("PAGO-004", error.getCode());
    }

    @Test
    void crearConTimeoutDelProveedorQuedaEnProcesoParaConfirmar() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> {
            Transaccion t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(12L);
                t.setCreatedAt(Instant.now());
                t.setUpdatedAt(Instant.now());
            }
            return t;
        });
        when(gestoPagoTxClient.sendTx(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <RESPONSE>
                            <MENSAJE>
                                <CODIGO>82</CODIGO>
                                <TEXTO>Timeout alcanzado, transaccion no registrada</TEXTO>
                            </MENSAJE>
                        </RESPONSE>
                        """);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("EN_PROCESO", dto.getEstado());
        assertTrue(dto.getErrorMensaje().contains("Timeout"));
    }

    @Test
    void crearConRespuestaDuplicadaAplicadaSeRecuperaComoAprobada() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> {
            Transaccion t = inv.getArgument(0);
            t.setId(13L);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });
        when(gestoPagoTxClient.sendTx(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <RESPONSE>
                            <NUM_AUTORIZACION>100057766</NUM_AUTORIZACION>
                            <MENSAJE>
                                <CODIGO>06</CODIGO>
                                <TEXTO>Upc ya fue registrado</TEXTO>
                            </MENSAJE>
                        </RESPONSE>
                        """);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("APROBADA", dto.getEstado());
        assertEquals("100057766", dto.getNumeroAutorizacion());
    }

    @Test
    void confirmarAntesDe62SegundosNoLlamaAlProveedor() {
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(20L);
        tx.setCreatedAt(Instant.now().minusSeconds(10));
        when(transaccionRepository.findByIdAndUsuarioId(20L, 1L)).thenReturn(Optional.of(tx));

        TransaccionDto dto = service.confirmarTransaccion(1L, 20L);

        assertEquals("EN_PROCESO", dto.getEstado());
        verify(gestoPagoTxClient, never()).confirmTx(anyString(), any());
    }

    @Test
    void confirmarExitosoMarcaAprobada() {
        CatalogoProductoCache p = recarga();
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(p));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(21L);
        tx.setCreatedAt(Instant.now().minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(21L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any())).thenReturn(XML_CONFIRM_EXITO);

        TransaccionDto dto = service.confirmarTransaccion(1L, 21L);

        assertEquals("APROBADA", dto.getEstado());
        assertEquals("100024988", dto.getNumeroAutorizacion());
        assertEquals("291437188", dto.getIdTx());
    }

    @Test
    void confirmarNoAplicadaMarcaFallida() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(22L);
        tx.setCreatedAt(Instant.now().minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(22L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any())).thenReturn(XML_CONFIRM_NO_APLICADA);

        TransaccionDto dto = service.confirmarTransaccion(1L, 22L);

        assertEquals("FALLIDA", dto.getEstado());
        assertTrue(dto.getErrorMensaje().contains("no fue aplicada"));
    }

    @Test
    void confirmarDeTransaccionInexistenteEsRechazada() {
        when(transaccionRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.confirmarTransaccion(1L, 99L));
        assertEquals("PAGO-005", error.getCode());
    }

    @Test
    void confirmarTransaccionYaFinalizadaNoLlamaraAlProveedor() {
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.FALLIDA, "clave-idem-123");
        tx.setId(23L);
        when(transaccionRepository.findByIdAndUsuarioId(23L, 1L)).thenReturn(Optional.of(tx));

        TransaccionDto dto = service.confirmarTransaccion(1L, 23L);

        assertEquals("FALLIDA", dto.getEstado());
        verify(gestoPagoTxClient, never()).confirmTx(anyString(), any());
    }

    @Test
    void historialOrdenaMasRecientesPrimero() {
        Transaccion a = transaccion(76, 205, EstadoTransaccion.APROBADA, "k1");
        a.setId(1L);
        Transaccion b = transaccion(108, 272, EstadoTransaccion.PENDIENTE, "k2");
        b.setId(2L);
        when(transaccionRepository.findByUsuarioIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(b, a));

        List<TransaccionDto> historial = service.historial(1L);

        assertEquals(2, historial.size());
        assertEquals("PENDIENTE", historial.get(0).getEstado());
        assertEquals("APROBADA", historial.get(1).getEstado());
    }

    private Transaccion transaccion(Integer idServicio, Integer idProducto,
                                    EstadoTransaccion estado, String clave) {
        Transaccion tx = new Transaccion();
        tx.setUsuarioId(1L);
        tx.setIdServicio(idServicio);
        tx.setIdProducto(idProducto);
        tx.setServicio("Servicio");
        tx.setProducto("Producto");
        tx.setReferencia("5577777777");
        tx.setMonto(new BigDecimal("100.00"));
        tx.setComision(BigDecimal.ZERO);
        tx.setEstado(estado);
        tx.setIdempotencyKey(clave);
        tx.setUpc("clave-idem-123");
        tx.setUnidad("083");
        tx.setCreatedAt(Instant.now().minusSeconds(300));
        tx.setUpdatedAt(Instant.now().minusSeconds(300));
        return tx;
    }
}