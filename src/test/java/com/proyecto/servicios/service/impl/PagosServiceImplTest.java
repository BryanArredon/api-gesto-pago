package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetencionCuentaResponse;
import com.proyecto.servicios.model.pago.PagoRequestDto;
import com.proyecto.servicios.model.pago.TransaccionDto;
import com.proyecto.servicios.model.pago.VerificarReferenciaRequest;
import com.proyecto.servicios.model.pago.VerificarReferenciaResponse;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import com.proyecto.servicios.repositorys.pago.EventoTransaccionRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionRepository;
import com.proyecto.servicios.service.BloqueoIdempotencia;
import com.proyecto.servicios.service.CatalogoConsulta;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ServicioRetencionBancaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagosServiceImplTest {

    private static final String CUENTA_LUHN_VALIDA = "0000000018";
    private static final String PIN_VALIDO = "4829";
    private static final UUID FOLIO_RETENCION = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant AHORA_FIJO = Instant.parse("2026-04-20T18:00:00Z");

    private static final String XML_SENDTX_EXITO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <AUTORIZACION>
                <ID_TX>1975170643</ID_TX>
                <NUM_AUTORIZACION>100048015</NUM_AUTORIZACION>
                <SALDO>30484.32</SALDO>
                <COMISION>6.0</COMISION>
                <FECHA>12/Mar/2024 15:10:48</FECHA>
                <MONTO>701.99</MONTO>
                <CODIGO_MENSAJE>01</CODIGO_MENSAJE>
                <MENSAJE>Operacion realizada con exito</MENSAJE>
                <REFERENCIA>25720381</REFERENCIA>
            </AUTORIZACION>
            """;
    private static final String XML_SENDTX_CARRIER_DOWN = """
            <?xml version='1.0' encoding='UTF-8'?>
            <AUTORIZACION> <NUM_AUTORIZACION>-1</NUM_AUTORIZACION><CODIGO_MENSAJE>03</CODIGO_MENSAJE><MENSAJE>Por el momento el carrier no responde, favor de intentar mas tarde.</MENSAJE></AUTORIZACION>
            """;
    private static final String XML_CONFIRM_DISPOSITIVO_NO_REGISTRADO = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE> <NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>51</CODIGO><TEXTO>Dispositivo no registrado</TEXTO></MENSAJE></RESPONSE>
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
    private static final String XML_CONFIRM_TIMEOUT = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>82</CODIGO>
                    <TEXTO>Timeout consultando confirmTx</TEXTO>
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
    @Mock
    private ServicioRetencionBancaria servicioRetencionBancaria;
    @Mock
    private UsuarioRepository usuarioRepository;

    private Clock clock;
    private PagosServiceImpl service;
    private GestoPagoToken token;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(AHORA_FIJO, ZoneOffset.UTC);
        service = new PagosServiceImpl(
                gestoPagoTokenService,
                gestoPagoTxClient,
                transaccionRepository,
                eventoTransaccionRepository,
                catalogoConsulta,
                bloqueoIdempotencia,
                servicioRetencionBancaria,
                usuarioRepository,
                clock,
                1,
                "DISP01",
                "083",
                62);
        token = new GestoPagoToken();
        token.setToken("token-de-prueba");
        lenient().when(gestoPagoTokenService.obtenerTokenActivo(1, "DISP01"))
                .thenReturn(Optional.of(token));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("cliente@banco.mx");
        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        RetencionCuentaResponse retencionExitosa = new RetencionCuentaResponse(
                FOLIO_RETENCION,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "******0018",
                "clave-idem-123",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "MXN",
                "Pago Telcel - Recarga 100",
                "RETENIDO",
                null,
                null,
                AHORA_FIJO.plusSeconds(300),
                AHORA_FIJO,
                AHORA_FIJO);
        lenient().when(servicioRetencionBancaria.retenerSaldo(
                        anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(retencionExitosa);
        lenient().when(servicioRetencionBancaria.reversarRetencionConReintento(anyString(), any(), anyString()))
                .thenReturn(true);
    }

    private void stubPersistenciaTransaccion(Long idInicial) {
        final Transaccion[] referenciaGuardada = new Transaccion[1];
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> {
            Transaccion t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(idInicial);
                t.setCreatedAt(AHORA_FIJO);
            }
            t.setUpdatedAt(AHORA_FIJO);
            referenciaGuardada[0] = t;
            return t;
        });
        lenient().when(transaccionRepository.findById(idInicial))
                .thenAnswer(inv -> Optional.ofNullable(referenciaGuardada[0]));
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
        p.setPrecio(new BigDecimal("6.00"));
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
        r.setNumeroCuenta(CUENTA_LUHN_VALIDA);
        r.setPinAutorizacion(PIN_VALIDO);
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
        VerificarReferenciaRequest req = request(76, 205, "5577777777");

        ApiException error = assertThrows(ApiException.class,
                () -> service.verificarReferencia(req));
        assertEquals("PAGO-002", error.getCode());
        verify(gestoPagoTxClient, never()).verifyReference(anyString(), any());
    }

    @Test
    void crearRechazaNumeroCuentaConLuhnInvalidoSinRetenerSaldo() {
        PagoRequestDto req = pago(recarga(), "5577777777", null);
        req.setNumeroCuenta("0000000019"); // Luhn invalido

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, req));

        assertEquals("PAGO-007", error.getCode());
        verify(servicioRetencionBancaria, never())
                .retenerSaldo(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
        verify(gestoPagoTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    void crearRechazaPinConFormatoInvalidoSinRetenerSaldo() {
        PagoRequestDto req = pago(recarga(), "5577777777", null);
        req.setPinAutorizacion("12A4");

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, req));

        assertEquals("PAGO-008", error.getCode());
        verify(servicioRetencionBancaria, never())
                .retenerSaldo(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void crearRecargaExitosaRetieneLiquidaYMarcaLiquidado() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(10L);
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_EXITO);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("LIQUIDADO", dto.getEstado());
        assertEquals("******0018", dto.getNumeroCuentaEnmascarado());
        assertEquals(FOLIO_RETENCION.toString(), dto.getFolioRetencion());
        assertEquals("100048015", dto.getNumeroAutorizacion());
        assertEquals("1975170643", dto.getIdTx());
        assertEquals("701.99", dto.getMonto());
        assertEquals("6.00", dto.getComision());
        assertEquals("clave-idem-123", dto.getUpc());

        verify(servicioRetencionBancaria).retenerSaldo(
                eq("cliente@banco.mx"),
                eq(CUENTA_LUHN_VALIDA),
                eq(PIN_VALIDO),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("0.00")),
                anyString(),
                eq("clave-idem-123"));
        verify(servicioRetencionBancaria).liquidarRetencion(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                eq("100048015"));

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
        stubPersistenciaTransaccion(11L);
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
    void crearTransaccionDuplicadaConMismoPayloadDevuelveExistenteSinRetenerNiLlamarAlProveedor() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion existente = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.of(existente));

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("EN_PROCESO", dto.getEstado());
        verify(servicioRetencionBancaria, never())
                .retenerSaldo(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
        verify(gestoPagoTxClient, never()).sendTx(anyString(), any());
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void crearTransaccionDuplicadaConPayloadDistintoDevuelveConflicto409() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion existente = transaccion(76, 205, EstadoTransaccion.LIQUIDADO, "clave-idem-123");
        existente.setPayloadHash("hash-de-otro-payload-distinto");
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.of(existente));

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pago(recarga(), "5577777777", null)));

        assertEquals("PAGO-006", error.getCode());
        assertEquals(409, error.getStatus().value());
        verify(servicioRetencionBancaria, never())
                .retenerSaldo(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString());
        verify(gestoPagoTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    void crearReintentaTransaccionReversadaReutilizandoLaFila() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion reversada = transaccion(76, 205, EstadoTransaccion.REVERSADO, "clave-idem-123");
        reversada.setId(15L);
        reversada.setErrorMensaje("Carrier no responde");
        reversada.setUpc("clave-idem-123");
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.of(reversada));
        when(transaccionRepository.findById(15L)).thenReturn(Optional.of(reversada));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_EXITO);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("LIQUIDADO", dto.getEstado());
        assertEquals("clave-idem-123", dto.getUpc());
    }

    @Test
    void crearConFalloEnHoldBancarioPorSaldoInsuficienteNoLlamaASendTx() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(16L);
        when(servicioRetencionBancaria.retenerSaldo(
                        anyString(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenThrow(ApiException.saldoInsuficiente());

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pago(recarga(), "5577777777", null)));

        assertEquals("PAGO-010", error.getCode());
        verify(gestoPagoTxClient, never()).sendTx(anyString(), any());
    }

    @Test
    void crearConProductoNoActivoEsRechazado() {
        when(catalogoConsulta.buscarActivo(999, 1)).thenReturn(Optional.empty());
        CatalogoProductoCache p = new CatalogoProductoCache();
        p.setIdServicio(999);
        p.setIdProducto(1);
        PagoRequestDto pagoReq = pago(p, "12345", new BigDecimal("50.00"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pagoReq));
        assertEquals("PAGO-003", error.getCode());
    }

    @Test
    void crearServicioSinMontoEsRechazado() {
        when(catalogoConsulta.buscarActivo(108, 272)).thenReturn(Optional.of(servicio()));
        PagoRequestDto pagoReq = pago(servicio(), "5610440665", null);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearTransaccion(1L, pagoReq));
        assertEquals("PAGO-004", error.getCode());
    }

    @Test
    void crearConTimeoutDelProveedorMantieneHoldYQuedaEnProcesoSinReversar() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(12L);
        when(gestoPagoTxClient.sendTx(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <AUTORIZACION>
                            <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
                            <CODIGO_MENSAJE>82</CODIGO_MENSAJE>
                            <MENSAJE>Timeout alcanzado, transaccion no registrada</MENSAJE>
                        </AUTORIZACION>
                        """);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("EN_PROCESO", dto.getEstado());
        assertTrue(dto.getErrorMensaje().contains("Timeout"));
        verify(servicioRetencionBancaria, never())
                .reversarRetencionConReintento(anyString(), any(), anyString());
    }

    @Test
    void crearConCarrierNoRespondeEjecutaReversoCompensatorioYMarcaReversado() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(14L);
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_CARRIER_DOWN);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("REVERSADO", dto.getEstado());
        assertTrue(dto.getErrorMensaje().contains("carrier no responde"));
        verify(servicioRetencionBancaria).reversarRetencionConReintento(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                anyString());
    }

    @Test
    void crearConRechazoDelProveedorYFalloEnReversoBancarioEscalaARequiereRevision() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(17L);
        when(gestoPagoTxClient.sendTx(anyString(), any())).thenReturn(XML_SENDTX_CARRIER_DOWN);
        when(servicioRetencionBancaria.reversarRetencionConReintento(anyString(), any(), anyString()))
                .thenReturn(false);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("REQUIERE_REVISION", dto.getEstado());
        verify(servicioRetencionBancaria).marcarParaRevision(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                anyString());
    }

    @Test
    void crearConRespuestaDuplicadaAplicadaSeRecuperaComoLiquidado() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        when(transaccionRepository.findByUsuarioIdAndIdempotencyKey(1L, "clave-idem-123"))
                .thenReturn(Optional.empty());
        stubPersistenciaTransaccion(13L);
        when(gestoPagoTxClient.sendTx(anyString(), any()))
                .thenReturn("""
                        <?xml version='1.0' encoding='UTF-8'?>
                        <AUTORIZACION>
                            <NUM_AUTORIZACION>100057766</NUM_AUTORIZACION>
                            <CODIGO_MENSAJE>06</CODIGO_MENSAJE>
                            <MENSAJE>Upc ya fue registrado</MENSAJE>
                        </AUTORIZACION>
                        """);

        TransaccionDto dto = service.crearTransaccion(1L, pago(recarga(), "5577777777", null));

        assertEquals("LIQUIDADO", dto.getEstado());
        assertEquals("100057766", dto.getNumeroAutorizacion());
    }

    @Test
    void confirmarAntesDe62SegundosNoLlamaAlProveedor() {
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(20L);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(10));
        when(transaccionRepository.findByIdAndUsuarioId(20L, 1L)).thenReturn(Optional.of(tx));

        TransaccionDto dto = service.confirmarTransaccion(1L, 20L);

        assertEquals("EN_PROCESO", dto.getEstado());
        verify(gestoPagoTxClient, never()).confirmTx(anyString(), any());
    }

    @Test
    void confirmarExitosoLiquidaHoldYMarcaLiquidado() {
        CatalogoProductoCache p = recarga();
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(p));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(21L);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(21L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.findById(21L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any())).thenReturn(XML_CONFIRM_EXITO);

        TransaccionDto dto = service.confirmarTransaccion(1L, 21L);

        assertEquals("LIQUIDADO", dto.getEstado());
        assertEquals("100024988", dto.getNumeroAutorizacion());
        assertEquals("291437188", dto.getIdTx());
        verify(servicioRetencionBancaria).liquidarRetencion(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                eq("100024988"));
    }

    @Test
    void confirmarNoAplicadaReversaHoldYMarcaReversado() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(22L);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(22L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.findById(22L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any())).thenReturn(XML_CONFIRM_NO_APLICADA);

        TransaccionDto dto = service.confirmarTransaccion(1L, 22L);

        assertEquals("REVERSADO", dto.getEstado());
        assertTrue(dto.getErrorMensaje().contains("no fue aplicada"));
        verify(servicioRetencionBancaria).reversarRetencionConReintento(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                anyString());
    }

    @Test
    void confirmarDispositivoNoRegistradoReversaHoldYMarcaReversado() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(24L);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(24L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.findById(24L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any()))
                .thenReturn(XML_CONFIRM_DISPOSITIVO_NO_REGISTRADO);

        TransaccionDto dto = service.confirmarTransaccion(1L, 24L);

        assertEquals("REVERSADO", dto.getEstado());
        assertEquals("Dispositivo no registrado", dto.getErrorMensaje());
    }

    @Test
    void confirmarConTimeoutEscalaTiemposYAlTercerIntentoMarcaRequiereRevision() {
        when(catalogoConsulta.buscarActivo(76, 205)).thenReturn(Optional.of(recarga()));
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.EN_PROCESO, "clave-idem-123");
        tx.setId(25L);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(120));
        when(transaccionRepository.findByIdAndUsuarioId(25L, 1L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.findById(25L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gestoPagoTxClient.confirmTx(anyString(), any())).thenReturn(XML_CONFIRM_TIMEOUT);

        // Intento 1 (a los 62s) -> programa siguiente confirmacion en +3 min
        TransaccionDto intento1 = service.confirmarTransaccion(1L, 25L);
        assertEquals("EN_PROCESO", intento1.getEstado());
        assertEquals(1, tx.getIntentosConfirmacion());
        assertEquals(AHORA_FIJO.plus(Duration.ofMinutes(3)), tx.getProximaConfirmacionEn());

        // Intento 2 (a los 3 min) -> programa siguiente confirmacion en +10 min
        TransaccionDto intento2 = service.confirmarTransaccion(1L, 25L);
        assertEquals("EN_PROCESO", intento2.getEstado());
        assertEquals(2, tx.getIntentosConfirmacion());
        assertEquals(AHORA_FIJO.plus(Duration.ofMinutes(10)), tx.getProximaConfirmacionEn());

        // Intento 3 (a los 10 min) -> sigue indeterminada -> pasa a REQUIERE_REVISION sin reversar
        TransaccionDto intento3 = service.confirmarTransaccion(1L, 25L);
        assertEquals("REQUIERE_REVISION", intento3.getEstado());
        verify(servicioRetencionBancaria).marcarParaRevision(
                eq("cliente@banco.mx"),
                eq(FOLIO_RETENCION),
                anyString());
        verify(servicioRetencionBancaria, never())
                .reversarRetencionConReintento(anyString(), any(), anyString());
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
        Transaccion tx = transaccion(76, 205, EstadoTransaccion.REVERSADO, "clave-idem-123");
        tx.setId(23L);
        when(transaccionRepository.findByIdAndUsuarioId(23L, 1L)).thenReturn(Optional.of(tx));

        TransaccionDto dto = service.confirmarTransaccion(1L, 23L);

        assertEquals("REVERSADO", dto.getEstado());
        verify(gestoPagoTxClient, never()).confirmTx(anyString(), any());
    }

    @Test
    void historialOrdenaMasRecientesPrimero() {
        Transaccion a = transaccion(76, 205, EstadoTransaccion.LIQUIDADO, "k1");
        a.setId(1L);
        Transaccion b = transaccion(108, 272, EstadoTransaccion.PENDIENTE, "k2");
        b.setId(2L);
        when(transaccionRepository.findByUsuarioIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(b, a));

        List<TransaccionDto> historial = service.historial(1L);

        assertEquals(2, historial.size());
        assertEquals("PENDIENTE", historial.get(0).getEstado());
        assertEquals("LIQUIDADO", historial.get(1).getEstado());
    }

    private Transaccion transaccion(Integer idServicio, Integer idProducto,
                                    EstadoTransaccion estado, String clave) {
        Transaccion tx = new Transaccion();
        tx.setUsuarioId(1L);
        tx.setNumeroCuenta(CUENTA_LUHN_VALIDA);
        tx.setFolioRetencion(FOLIO_RETENCION);
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
        tx.setIntentosConfirmacion(0);
        tx.setCreatedAt(AHORA_FIJO.minusSeconds(300));
        tx.setUpdatedAt(AHORA_FIJO.minusSeconds(300));
        return tx;
    }
}
