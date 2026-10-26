package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.gestopago.GestoPagoMensajeOperacion;
import com.proyecto.servicios.model.gestopago.GestoPagoOperacionResponse;
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
import com.proyecto.servicios.service.PagosService;
import com.proyecto.servicios.service.ServicioRetencionBancaria;
import com.proyecto.servicios.service.impl.TransaccionPersistenciaService.ResultadoPreparacion;
import com.proyecto.servicios.util.ValidadorLuhn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PagosServiceImpl implements PagosService {

    private static final String CODIGO_EXITO = "01";
    private static final String CODIGO_DUPLICADO = "06";
    private static final String CODIGO_NO_APLICADA = "70";
    private static final String CODIGO_TIMEOUT = "82";
    private static final String NO_AUTORIZACION = "-1";
    private static final String ORIGEN_PROVEEDOR = "PROVEEDOR";
    private static final String ORIGEN_CONCILIACION = "CONCILIACION";
    private static final int MAX_INTENTOS_CONFIRMACION = 3;
    private static final Pattern PATRON_PIN_CUATRO_DIGITOS = Pattern.compile("^[0-9]{4}$");

    public static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter HORA_LOCAL =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZONA_MEXICO);
    private static final DateTimeFormatter ZONA_HORARIA =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm:ss", Locale.ENGLISH);

    private static final Duration SEGUNDO_ESCALON_CONFIRMACION = Duration.ofMinutes(3);
    private static final Duration TERCER_ESCALON_CONFIRMACION = Duration.ofMinutes(10);

    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoTxClient gestoPagoTxClient;
    private final TransaccionRepository transaccionRepository;
    private final CatalogoConsulta catalogoConsulta;
    private final ServicioRetencionBancaria servicioRetencionBancaria;
    private final TransaccionPersistenciaService persistencia;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String unidad;
    private final Duration esperaConfirmacion;

    @Autowired
    public PagosServiceImpl(
            GestoPagoTokenService gestoPagoTokenService,
            GestoPagoTxClient gestoPagoTxClient,
            TransaccionRepository transaccionRepository,
            CatalogoConsulta catalogoConsulta,
            ServicioRetencionBancaria servicioRetencionBancaria,
            TransaccionPersistenciaService persistencia,
            UsuarioRepository usuarioRepository,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.unidad:083}") String unidad,
            @Value("${gestopago.confirm-wait-seconds:62}") long esperaConfirmacionSegundos) {
        this(
                gestoPagoTokenService,
                gestoPagoTxClient,
                transaccionRepository,
                catalogoConsulta,
                servicioRetencionBancaria,
                persistencia,
                usuarioRepository,
                Clock.systemUTC(),
                idDistribuidor,
                codigoDispositivo,
                unidad,
                esperaConfirmacionSegundos);
    }

    public PagosServiceImpl(
            GestoPagoTokenService gestoPagoTokenService,
            GestoPagoTxClient gestoPagoTxClient,
            TransaccionRepository transaccionRepository,
            EventoTransaccionRepository eventoTransaccionRepository,
            CatalogoConsulta catalogoConsulta,
            BloqueoIdempotencia bloqueoIdempotencia,
            ServicioRetencionBancaria servicioRetencionBancaria,
            UsuarioRepository usuarioRepository,
            Clock clock,
            Integer idDistribuidor,
            String codigoDispositivo,
            String unidad,
            long esperaConfirmacionSegundos) {
        this(
                gestoPagoTokenService,
                gestoPagoTxClient,
                transaccionRepository,
                catalogoConsulta,
                servicioRetencionBancaria,
                new TransaccionPersistenciaService(
                        transaccionRepository, eventoTransaccionRepository, bloqueoIdempotencia),
                usuarioRepository,
                clock,
                idDistribuidor,
                codigoDispositivo,
                unidad,
                esperaConfirmacionSegundos);
    }

    private PagosServiceImpl(
            GestoPagoTokenService gestoPagoTokenService,
            GestoPagoTxClient gestoPagoTxClient,
            TransaccionRepository transaccionRepository,
            CatalogoConsulta catalogoConsulta,
            ServicioRetencionBancaria servicioRetencionBancaria,
            TransaccionPersistenciaService persistencia,
            UsuarioRepository usuarioRepository,
            Clock clock,
            Integer idDistribuidor,
            String codigoDispositivo,
            String unidad,
            long esperaConfirmacionSegundos) {
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoTxClient = gestoPagoTxClient;
        this.transaccionRepository = transaccionRepository;
        this.catalogoConsulta = catalogoConsulta;
        this.servicioRetencionBancaria = servicioRetencionBancaria;
        this.persistencia = persistencia;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.unidad = unidad;
        this.esperaConfirmacion = Duration.ofSeconds(esperaConfirmacionSegundos);
    }

    @Override
    public VerificarReferenciaResponse verificarReferencia(VerificarReferenciaRequest request) {
        CatalogoProductoCache producto = productoOExcepcion(request.getIdServicio(), request.getIdProducto());
        if (producto.getTipoFront() == null || producto.getTipoFront() != 4) {
            throw ApiException.referenciaNoVerificable();
        }

        GestoPagoOperacionResponse respuesta = operar(() -> gestoPagoTxClient.verifyReference(token(), Map.of(
                "idProducto", request.getIdProducto().toString(),
                "idServicio", request.getIdServicio().toString(),
                "referencia", request.getReferencia())));
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String codigo = mensaje == null ? null : mensaje.getCodigo();
        if (CODIGO_EXITO.equals(codigo)) {
            return VerificarReferenciaResponse.valida(mensaje.getSaldo(), mensaje.getTexto());
        }
        return VerificarReferenciaResponse.invalida(
                mensaje == null ? "La referencia no es valida" : mensaje.getTexto());
    }

    @Override
    public TransaccionDto crearTransaccion(Long usuarioId, PagoRequestDto request) {
        validarSolicitudEntrada(request);
        String numeroCuenta = request.getNumeroCuenta().trim();
        String referencia = request.getReferencia().trim();
        String idempotencyKey = request.getIdempotencyKey().trim();

        CatalogoProductoCache producto = productoOExcepcion(request.getIdServicio(), request.getIdProducto());
        BigDecimal monto = montoAutorizado(producto, request.getMonto());
        BigDecimal comision = comisionAutorizada(producto);
        String payloadHash = calcularHashPayload(
                numeroCuenta, producto.getIdServicio(), producto.getIdProducto(), referencia, monto);

        // Etapa 1: Idempotencia y registro inicial PENDIENTE en transaccion de BD corta
        ResultadoPreparacion preparacion = persistencia.iniciarIntencionPendiente(
                usuarioId, numeroCuenta, producto, referencia, monto, comision, idempotencyKey, payloadHash, unidad);
        if (preparacion.yaProcesada()) {
            return TransaccionDto.from(preparacion.transaccion());
        }
        Transaccion transaccion = preparacion.transaccion();

        // Etapa 2: Validacion bancaria (Cuenta activa, PIN 4 digitos, Saldo >= Monto + Comision) y Hold
        String emailTitular = obtenerEmailUsuario(usuarioId);
        String concepto = "Pago " + producto.getServicio() + " - " + producto.getProducto();
        RetencionCuentaResponse retencion = servicioRetencionBancaria.retenerSaldo(
                emailTitular,
                numeroCuenta,
                request.getPinAutorizacion().trim(),
                monto,
                comision,
                concepto,
                idempotencyKey);

        // Etapa 3: Confirmar estado RETENIDO con el FolioMovimiento bancario antes de llamar al Switch
        Instant expiraHold = retencion != null && retencion.expiraEn() != null
                ? retencion.expiraEn()
                : clock.instant().plus(esperaConfirmacion);
        UUID folioMovimiento = retencion == null ? null : retencion.folioMovimiento();
        transaccion = persistencia.confirmarRetencionBancaria(transaccion.getId(), folioMovimiento, expiraHold);

        // Etapa 4: Ejecucion ante Switch GestoPago (fuera de transaccion de BD) y liquidacion/reverso
        String upc = transaccion.getUpc();
        try {
            GestoPagoOperacionResponse respuesta = operar(() -> gestoPagoTxClient.sendTx(
                    token(), cuerpoSendTx(producto, referencia, monto, upc)));
            transaccion = resolverResultadoSendTx(transaccion, emailTitular, respuesta);
        } catch (Exception e) {
            log.warn(
                    "sendTx incierto para transaccion id={} cuenta={}; saldo permanece RETENIDO y pasa a EN_PROCESO",
                    transaccion.getId(),
                    ValidadorLuhn.enmascararCuenta(numeroCuenta),
                    e);
            Instant proximaConfirmacion = clock.instant().plus(esperaConfirmacion);
            transaccion = persistencia.programarConfirmacionEnProceso(
                    transaccion.getId(),
                    "El proveedor no respondio; el resultado se confirmara despues",
                    0,
                    proximaConfirmacion,
                    "API");
        }
        return TransaccionDto.from(transaccion);
    }

    @Override
    public TransaccionDto confirmarTransaccion(Long usuarioId, Long id) {
        Transaccion transaccion = transaccionRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ApiException("PAGO-005", "Transaccion no encontrada", HttpStatus.NOT_FOUND));
        if (!transaccion.getEstado().permiteConfirmacion()) {
            return TransaccionDto.from(transaccion);
        }

        Instant ahora = clock.instant();
        Instant creada = transaccion.getCreatedAt();
        if (creada != null && Duration.between(creada, ahora).compareTo(esperaConfirmacion) < 0) {
            return TransaccionDto.from(transaccion);
        }

        Transaccion actualizada = ejecutarConfirmacionContraProveedor(transaccion, ORIGEN_PROVEEDOR);
        return TransaccionDto.from(actualizada);
    }

    /**
     * Ejecuta la consulta de estado real ({@code confirmTx.do}) para el worker de confirmacion escalonada
     * y el job TTL de holds huerfanos.
     */
    public Transaccion conciliarTransaccionPendiente(Transaccion transaccion) {
        if (!transaccion.getEstado().permiteConfirmacion()) {
            return transaccion;
        }
        return ejecutarConfirmacionContraProveedor(transaccion, ORIGEN_CONCILIACION);
    }

    @Override
    public List<TransaccionDto> historial(Long usuarioId) {
        return transaccionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(TransaccionDto::from)
                .toList();
    }

    private Transaccion ejecutarConfirmacionContraProveedor(Transaccion transaccion, String origen) {
        CatalogoProductoCache producto = productoOExcepcion(transaccion.getIdServicio(), transaccion.getIdProducto());
        String emailTitular = obtenerEmailUsuario(transaccion.getUsuarioId());
        int intentoActual = transaccion.getIntentosConfirmacion() + 1;

        try {
            GestoPagoOperacionResponse respuesta = operar(() -> gestoPagoTxClient.confirmTx(
                    token(), cuerpoSendTx(producto, transaccion.getReferencia(),
                            transaccion.getMonto(), transaccion.getUpc())));
            return resolverResultadoConfirmTx(transaccion, emailTitular, respuesta, intentoActual, origen);
        } catch (Exception e) {
            log.warn(
                    "confirmTx intento {}/{} no concluyo para transaccion id={}",
                    intentoActual,
                    MAX_INTENTOS_CONFIRMACION,
                    transaccion.getId(),
                    e);
            return manejarConfirmacionIncierta(
                    transaccion,
                    emailTitular,
                    intentoActual,
                    "El proveedor no respondio durante confirmTx",
                    origen);
        }
    }

    private Transaccion resolverResultadoSendTx(
            Transaccion transaccion, String emailTitular, GestoPagoOperacionResponse respuesta) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String codigo = mensaje == null ? null : mensaje.getCodigo();
        String texto = mensaje == null ? "El proveedor rechazo la operacion" : mensaje.getTexto();

        if (CODIGO_EXITO.equals(codigo) || esDuplicadoAplicado(respuesta)) {
            return liquidarTransaccionAprobada(
                    transaccion,
                    emailTitular,
                    respuesta,
                    ORIGEN_PROVEEDOR,
                    mensaje == null ? "Operacion realizada con exito" : texto);
        }
        if (CODIGO_TIMEOUT.equals(codigo)) {
            Instant proximaConfirmacion = clock.instant().plus(esperaConfirmacion);
            return persistencia.programarConfirmacionEnProceso(
                    transaccion.getId(),
                    texto,
                    0,
                    proximaConfirmacion,
                    ORIGEN_PROVEEDOR);
        }
        return reversarPorRechazoDefinitivo(transaccion, emailTitular, texto, ORIGEN_PROVEEDOR);
    }

    private Transaccion resolverResultadoConfirmTx(
            Transaccion transaccion,
            String emailTitular,
            GestoPagoOperacionResponse respuesta,
            int intentoActual,
            String origen) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String codigo = mensaje == null ? null : mensaje.getCodigo();
        String texto = mensaje == null ? "Sin mensaje del proveedor" : mensaje.getTexto();

        if (CODIGO_EXITO.equals(codigo) || CODIGO_DUPLICADO.equals(codigo)) {
            return liquidarTransaccionAprobada(transaccion, emailTitular, respuesta, origen, texto);
        }
        if (CODIGO_TIMEOUT.equals(codigo)) {
            return manejarConfirmacionIncierta(transaccion, emailTitular, intentoActual, texto, origen);
        }
        if (CODIGO_NO_APLICADA.equals(codigo) || codigo != null) {
            return reversarPorRechazoDefinitivo(transaccion, emailTitular, texto, origen);
        }
        return manejarConfirmacionIncierta(transaccion, emailTitular, intentoActual, texto, origen);
    }

    private Transaccion liquidarTransaccionAprobada(
            Transaccion transaccion,
            String emailTitular,
            GestoPagoOperacionResponse respuesta,
            String origen,
            String detalle) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String numAutorizacion = respuesta.getNumAutorizacion();
        String idTx = primeroNoVacio(respuesta.getIdTx(), mensaje == null ? null : mensaje.getIdTx());

        servicioRetencionBancaria.liquidarRetencion(
                emailTitular, transaccion.getFolioRetencion(), primeroNoVacio(numAutorizacion, idTx));

        return persistencia.marcarLiquidada(
                transaccion.getId(),
                numAutorizacion,
                idTx,
                mensaje == null ? null : mensaje.getPin(),
                mensaje == null ? null : mensaje.getLegend(),
                decimal(respuesta.getMonto()),
                decimal(respuesta.getComision()),
                fecha(respuesta.getFecha()),
                origen,
                detalle);
    }

    private Transaccion reversarPorRechazoDefinitivo(
            Transaccion transaccion, String emailTitular, String motivo, String origen) {
        persistencia.iniciarReverso(transaccion.getId(), motivo, origen);
        boolean reversado = servicioRetencionBancaria.reversarRetencionConReintento(
                emailTitular, transaccion.getFolioRetencion(), motivo);
        if (reversado) {
            return persistencia.completarReverso(transaccion.getId(), motivo, origen);
        }
        String detalleRevision = "Fallo al reversar hold bancario tras rechazo del proveedor: " + motivo;
        servicioRetencionBancaria.marcarParaRevision(
                emailTitular, transaccion.getFolioRetencion(), detalleRevision);
        return persistencia.marcarRequiereRevision(transaccion.getId(), detalleRevision, origen);
    }

    private Transaccion manejarConfirmacionIncierta(
            Transaccion transaccion,
            String emailTitular,
            int intentoActual,
            String motivo,
            String origen) {
        if (intentoActual >= MAX_INTENTOS_CONFIRMACION) {
            String detalleRevision =
                    "Operacion indeterminada tras " + intentoActual + " intentos de confirmTx: " + motivo;
            servicioRetencionBancaria.marcarParaRevision(
                    emailTitular, transaccion.getFolioRetencion(), detalleRevision);
            return persistencia.marcarRequiereRevision(transaccion.getId(), detalleRevision, origen);
        }
        Duration esperaSiguiente = intentoActual == 1 ? SEGUNDO_ESCALON_CONFIRMACION : TERCER_ESCALON_CONFIRMACION;
        Instant proximaConfirmacion = clock.instant().plus(esperaSiguiente);
        return persistencia.programarConfirmacionEnProceso(
                transaccion.getId(), motivo, intentoActual, proximaConfirmacion, origen);
    }

    private void validarSolicitudEntrada(PagoRequestDto request) {
        if (request == null || request.getReferencia() == null || request.getReferencia().isBlank()) {
            throw ApiException.referenciaInvalida();
        }
        if (!ValidadorLuhn.esNumeroCuentaValido(request.getNumeroCuenta())) {
            throw ApiException.numeroCuentaInvalido();
        }
        if (request.getPinAutorizacion() == null
                || !PATRON_PIN_CUATRO_DIGITOS.matcher(request.getPinAutorizacion().trim()).matches()) {
            throw ApiException.pinAutorizacionInvalido("El PIN de autorizacion debe tener exactamente 4 digitos");
        }
    }

    private CatalogoProductoCache productoOExcepcion(Integer idServicio, Integer idProducto) {
        return catalogoConsulta.buscarActivo(idServicio, idProducto)
                .orElseThrow(ApiException::productoNoDisponible);
    }

    private BigDecimal montoAutorizado(CatalogoProductoCache producto, BigDecimal solicitado) {
        if (producto.getTipoFront() != null && producto.getTipoFront() == 1) {
            return producto.getPrecio().setScale(2, RoundingMode.HALF_EVEN);
        }
        if (solicitado == null || solicitado.compareTo(BigDecimal.ZERO) <= 0
                || solicitado.scale() > 2) {
            throw ApiException.montoInvalido();
        }
        return solicitado.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal comisionAutorizada(CatalogoProductoCache producto) {
        if (producto.getTipoFront() != null
                && producto.getTipoFront() != 1
                && producto.getPrecio() != null
                && producto.getPrecio().compareTo(BigDecimal.ZERO) > 0) {
            return producto.getPrecio().setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String calcularHashPayload(
            String numeroCuenta,
            Integer idServicio,
            Integer idProducto,
            String referencia,
            BigDecimal monto) {
        String canonico = numeroCuenta
                + "|" + idServicio
                + "|" + idProducto
                + "|" + referencia
                + "|" + monto.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de idempotencia", e);
        }
    }

    private String obtenerEmailUsuario(Long usuarioId) {
        if (usuarioRepository == null || usuarioId == null) {
            return "cliente@gestopago.mx";
        }
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getEmail)
                .orElse("cliente@gestopago.mx");
    }

    private boolean esDuplicadoAplicado(GestoPagoOperacionResponse respuesta) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        if (!CODIGO_DUPLICADO.equals(mensaje == null ? null : mensaje.getCodigo())) {
            return false;
        }
        return respuesta.getNumAutorizacion() != null
                && !respuesta.getNumAutorizacion().isBlank()
                && !NO_AUTORIZACION.equals(respuesta.getNumAutorizacion().trim());
    }

    private Map<String, String> cuerpoSendTx(CatalogoProductoCache producto, String referencia,
                                             BigDecimal monto, String upc) {
        boolean pagoServicio = producto.getTipoFront() != null
                && (producto.getTipoFront() == 2 || producto.getTipoFront() == 4);
        Map<String, String> body = new HashMap<>();
        body.put("idProducto", String.valueOf(producto.getIdProducto()));
        body.put("idServicio", String.valueOf(producto.getIdServicio()));
        body.put("telefono", telefono(producto, referencia, pagoServicio));
        body.put("horaLocal", HORA_LOCAL.format(clock.instant()));
        body.put("upc", upc);
        body.put("unidad", unidad);
        if (pagoServicio) {
            body.put("referencia", referencia);
            body.put("montoPago", monto.toPlainString());
        }
        return body;
    }

    private String telefono(CatalogoProductoCache producto, String referencia, boolean pagoServicio) {
        if (producto.getIdCatTipoServicio() != null
                && (producto.getIdCatTipoServicio() == 10 || producto.getIdCatTipoServicio() == 11)) {
            return "#1111111111";
        }
        if (pagoServicio) {
            return "1111111111";
        }
        return referencia;
    }

    private String token() {
        GestoPagoToken token = gestoPagoTokenService
                .obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseThrow(() -> new IllegalStateException("No hay token GestoPago guardado"));
        return "Bearer " + token.getToken();
    }

    private GestoPagoOperacionResponse operar(Operacion operacion) {
        return parsear(operacion.ejecutar());
    }

    private GestoPagoOperacionResponse parsear(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            GestoPagoOperacionResponse respuesta = new GestoPagoOperacionResponse();
            respuesta.setIdTx(texto(doc, "ID_TX"));
            respuesta.setNumAutorizacion(texto(doc, "NUM_AUTORIZACION"));
            respuesta.setSaldo(texto(doc, "SALDO"));
            respuesta.setComision(texto(doc, "COMISION"));
            respuesta.setFecha(texto(doc, "FECHA"));
            respuesta.setMonto(texto(doc, "MONTO"));
            respuesta.setMensaje(mensajeDe(doc));
            return respuesta;
        } catch (Exception e) {
            throw new IllegalStateException("Error al interpretar la respuesta de GestoPago", e);
        }
    }

    private GestoPagoMensajeOperacion mensajeDe(Document doc) {
        GestoPagoMensajeOperacion mensaje = new GestoPagoMensajeOperacion();
        mensaje.setCodigo(primeroNoVacio(texto(doc, "CODIGO_MENSAJE"), texto(doc, "CODIGO")));
        mensaje.setTexto(textoDeMensaje(doc.getElementsByTagName("MENSAJE")));
        mensaje.setSaldo(texto(doc, "SALDO"));
        mensaje.setReferencia(texto(doc, "REFERENCIA"));
        mensaje.setIdTx(texto(doc, "ID_TX"));
        mensaje.setPin(texto(doc, "PIN"));
        mensaje.setLegend(primeroNoVacio(texto(doc, "LEGEND"), texto(doc, "legend")));
        return mensaje;
    }

    private String texto(Document doc, String tag) {
        NodeList nodos = doc.getElementsByTagName(tag);
        if (nodos.getLength() == 0) {
            return null;
        }
        return nodos.item(0).getTextContent().trim();
    }

    private String textoDeMensaje(NodeList mensajes) {
        if (mensajes.getLength() == 0) {
            return null;
        }
        Element mensaje = (Element) mensajes.item(0);
        NodeList textos = mensaje.getElementsByTagName("TEXTO");
        if (textos.getLength() > 0) {
            return textos.item(0).getTextContent().trim();
        }
        return mensaje.getTextContent().trim();
    }

    private BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim()).setScale(2, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor.trim().replaceAll("\\s+", " "), ZONA_HORARIA)
                    .atZone(ZONA_MEXICO)
                    .toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String primeroNoVacio(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    @FunctionalInterface
    private interface Operacion {
        String ejecutar();
    }
}
