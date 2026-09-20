package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.EventoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.gestopago.GestoPagoMensajeOperacion;
import com.proyecto.servicios.model.gestopago.GestoPagoOperacionResponse;
import com.proyecto.servicios.model.idempotencia.ClaveIdempotencia;
import com.proyecto.servicios.model.pago.PagoRequestDto;
import com.proyecto.servicios.model.pago.TransaccionDto;
import com.proyecto.servicios.model.pago.VerificarReferenciaRequest;
import com.proyecto.servicios.model.pago.VerificarReferenciaResponse;
import com.proyecto.servicios.repositorys.pago.EventoTransaccionRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionRepository;
import com.proyecto.servicios.service.BloqueoIdempotencia;
import com.proyecto.servicios.service.CatalogoConsulta;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagosService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import java.util.Optional;

@Service
@Slf4j
public class PagosServiceImpl implements PagosService {

    private static final String CODIGO_EXITO = "01";
    private static final String CODIGO_DUPLICADO = "06";
    private static final String CODIGO_TIMEOUT = "82";
    private static final String NO_AUTORIZACION = "-1";
    private static final DateTimeFormatter HORA_LOCAL =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter ZONA_HORARIA =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm:ss", Locale.ENGLISH);

    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoTxClient gestoPagoTxClient;
    private final TransaccionRepository transaccionRepository;
    private final EventoTransaccionRepository eventoTransaccionRepository;
    private final CatalogoConsulta catalogoConsulta;
    private final BloqueoIdempotencia bloqueoIdempotencia;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String unidad;
    private final Duration esperaConfirmacion;

    public PagosServiceImpl(GestoPagoTokenService gestoPagoTokenService,
                            GestoPagoTxClient gestoPagoTxClient,
                            TransaccionRepository transaccionRepository,
                            EventoTransaccionRepository eventoTransaccionRepository,
                            CatalogoConsulta catalogoConsulta,
                            BloqueoIdempotencia bloqueoIdempotencia,
                            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
                            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
                            @Value("${gestopago.unidad:083}") String unidad,
                            @Value("${gestopago.confirm-wait-seconds:62}") long esperaConfirmacionSegundos) {
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoTxClient = gestoPagoTxClient;
        this.transaccionRepository = transaccionRepository;
        this.eventoTransaccionRepository = eventoTransaccionRepository;
        this.catalogoConsulta = catalogoConsulta;
        this.bloqueoIdempotencia = bloqueoIdempotencia;
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
    @Transactional
    public TransaccionDto crearTransaccion(Long usuarioId, PagoRequestDto request) {
        if (request.getReferencia() == null || request.getReferencia().isBlank()) {
            throw ApiException.referenciaInvalida();
        }
        CatalogoProductoCache producto = productoOExcepcion(request.getIdServicio(), request.getIdProducto());
        BigDecimal monto = montoAutorizado(producto, request.getMonto());

        ClaveIdempotencia clave = new ClaveIdempotencia(usuarioId, request.getIdempotencyKey());
        bloqueoIdempotencia.adquirir(clave);

        Optional<Transaccion> existente = transaccionRepository
                .findByUsuarioIdAndIdempotencyKey(usuarioId, request.getIdempotencyKey());
        if (existente.isPresent()
                && existente.get().getEstado() != EstadoTransaccion.FALLIDA
                && existente.get().getEstado() != EstadoTransaccion.RECHAZADA) {
            return TransaccionDto.from(existente.get());
        }

        Transaccion transaccion = prepararTransaccion(existente.orElse(null), usuarioId, producto,
                request.getReferencia(), monto, request.getIdempotencyKey(), clave);
        registrarEvento(transaccion.getId(), EstadoTransaccion.PENDIENTE, "API",
                "Transaccion registrada para envio al proveedor");

        try {
            GestoPagoOperacionResponse respuesta = operar(() -> gestoPagoTxClient.sendTx(
                    token(), cuerpoSendTx(producto, request.getReferencia(), monto, transaccion.getUpc())));
            aplicarResultadoSendTx(transaccion, respuesta);
        } catch (Exception e) {
            log.warn("sendTx no concluyo para transaccion id={}, quedara en proceso", transaccion.getId(), e);
            cambiarEstado(transaccion, EstadoTransaccion.EN_PROCESO, "API",
                    "El proveedor no respondio; el resultado se confirmara despues");
        }
        return TransaccionDto.from(transaccion);
    }

    @Override
    @Transactional
    public TransaccionDto confirmarTransaccion(Long usuarioId, Long id) {
        Transaccion transaccion = transaccionRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ApiException("PAGO-005", "Transaccion no encontrada", HttpStatus.NOT_FOUND));
        if (esEstadoFinal(transaccion.getEstado())) {
            return TransaccionDto.from(transaccion);
        }

        Instant creada = transaccion.getCreatedAt();
        if (creada != null && Duration.between(creada, Instant.now()).compareTo(esperaConfirmacion) < 0) {
            return TransaccionDto.from(transaccion);
        }

        CatalogoProductoCache producto = productoOExcepcion(transaccion.getIdServicio(), transaccion.getIdProducto());
        try {
            GestoPagoOperacionResponse respuesta = operar(() -> gestoPagoTxClient.confirmTx(
                    token(), cuerpoSendTx(producto, transaccion.getReferencia(),
                            transaccion.getMonto(), transaccion.getUpc())));
            aplicarResultadoConfirmTx(transaccion, respuesta);
        } catch (Exception e) {
            log.warn("confirmTx no concluyo para transaccion id={}, se mantiene en proceso", transaccion.getId(), e);
        }
        return TransaccionDto.from(transaccion);
    }

    @Override
    public List<TransaccionDto> historial(Long usuarioId) {
        return transaccionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(TransaccionDto::from)
                .toList();
    }

    private CatalogoProductoCache productoOExcepcion(Integer idServicio, Integer idProducto) {
        return catalogoConsulta.buscarActivo(idServicio, idProducto)
                .orElseThrow(ApiException::productoNoDisponible);
    }

    private BigDecimal montoAutorizado(CatalogoProductoCache producto, BigDecimal solicitado) {
        if (producto.getTipoFront() != null && producto.getTipoFront() == 1) {
            return producto.getPrecio();
        }
        if (solicitado == null || solicitado.compareTo(BigDecimal.ZERO) <= 0
                || solicitado.scale() > 2) {
            throw ApiException.montoInvalido();
        }
        return solicitado.setScale(2, RoundingMode.UNNECESSARY);
    }

    private Transaccion prepararTransaccion(Transaccion previa, Long usuarioId, CatalogoProductoCache producto,
                                            String referencia, BigDecimal monto, String idempotencyKey,
                                            ClaveIdempotencia clave) {
        Transaccion transaccion = previa != null ? previa : new Transaccion();
        if (transaccion.getId() == null) {
            transaccion.setUsuarioId(usuarioId);
            transaccion.setIdServicio(producto.getIdServicio());
            transaccion.setIdProducto(producto.getIdProducto());
            transaccion.setServicio(producto.getServicio());
            transaccion.setProducto(producto.getProducto());
            transaccion.setIdempotencyKey(idempotencyKey);
            transaccion.setUpc(upc(idempotencyKey));
            transaccion.setUnidad(unidad);
            transaccion.setComision(BigDecimal.ZERO);
        }
        transaccion.setReferencia(referencia);
        transaccion.setMonto(monto);
        transaccion.setEstado(EstadoTransaccion.PENDIENTE);
        transaccion.setErrorMensaje(null);
        return transaccionRepository.save(transaccion);
    }

    private void aplicarResultadoSendTx(Transaccion transaccion, GestoPagoOperacionResponse respuesta) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String codigo = mensaje == null ? null : mensaje.getCodigo();
        String texto = mensaje == null ? "El proveedor rechazo la operacion" : mensaje.getTexto();
        if (CODIGO_EXITO.equals(codigo) || esDuplicadoAplicado(respuesta)) {
            aplicarDatosExito(transaccion, respuesta);
            cambiarEstado(transaccion, EstadoTransaccion.APROBADA, "PROVEEDOR",
                    mensaje == null ? "Operacion realizada con exito" : texto);
            return;
        }
        if (CODIGO_TIMEOUT.equals(codigo)) {
            transaccion.setErrorMensaje(texto);
            cambiarEstado(transaccion, EstadoTransaccion.EN_PROCESO, "PROVEEDOR",
                    "Resultado incierto; se confirmara contra el proveedor despues");
            return;
        }
        transaccion.setErrorMensaje(texto);
        cambiarEstado(transaccion, EstadoTransaccion.FALLIDA, "PROVEEDOR", texto);
    }

    private void aplicarResultadoConfirmTx(Transaccion transaccion, GestoPagoOperacionResponse respuesta) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        String codigo = mensaje == null ? null : mensaje.getCodigo();
        String texto = mensaje == null ? "Sin mensaje del proveedor" : mensaje.getTexto();
        if (CODIGO_EXITO.equals(codigo) || CODIGO_DUPLICADO.equals(codigo)) {
            aplicarDatosExito(transaccion, respuesta);
            cambiarEstado(transaccion, EstadoTransaccion.APROBADA, "PROVEEDOR", texto);
            return;
        }
        transaccion.setErrorMensaje(texto);
        cambiarEstado(transaccion, EstadoTransaccion.FALLIDA, "PROVEEDOR", texto);
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

    private void aplicarDatosExito(Transaccion transaccion, GestoPagoOperacionResponse respuesta) {
        GestoPagoMensajeOperacion mensaje = respuesta.getMensaje();
        transaccion.setNumeroAutorizacion(respuesta.getNumAutorizacion());
        transaccion.setIdTx(primeroNoVacio(respuesta.getIdTx(), mensaje == null ? null : mensaje.getIdTx()));
        transaccion.setPin(mensaje == null ? null : mensaje.getPin());
        transaccion.setLegend(mensaje == null ? null : mensaje.getLegend());
        BigDecimal monto = decimal(respuesta.getMonto());
        if (monto != null) {
            transaccion.setMonto(monto);
        }
        BigDecimal comision = decimal(respuesta.getComision());
        if (comision != null) {
            transaccion.setComision(comision);
        }
        transaccion.setFecha(fecha(respuesta.getFecha()));
    }

    private Map<String, String> cuerpoSendTx(CatalogoProductoCache producto, String referencia,
                                             BigDecimal monto, String upc) {
        boolean pagoServicio = producto.getTipoFront() != null && producto.getTipoFront() == 2;
        Map<String, String> body = new HashMap<>();
        body.put("idProducto", String.valueOf(producto.getIdProducto()));
        body.put("idServicio", String.valueOf(producto.getIdServicio()));
        body.put("telefono", telefono(producto, referencia, pagoServicio));
        body.put("horaLocal", HORA_LOCAL.format(Instant.now()));
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

    private String upc(String idempotencyKey) {
        if (idempotencyKey.length() >= 3 && idempotencyKey.length() <= 15) {
            return idempotencyKey;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 15);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar el upc", e);
        }
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
            JAXBContext context = JAXBContext.newInstance(GestoPagoOperacionResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (GestoPagoOperacionResponse) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new IllegalStateException("Error al interpretar la respuesta de GestoPago", e);
        }
    }

    private boolean esEstadoFinal(EstadoTransaccion estado) {
        return estado == EstadoTransaccion.APROBADA
                || estado == EstadoTransaccion.FALLIDA
                || estado == EstadoTransaccion.RECHAZADA;
    }

    private void cambiarEstado(Transaccion transaccion, EstadoTransaccion estado, String origen, String detalle) {
        transaccion.setEstado(estado);
        transaccionRepository.save(transaccion);
        registrarEvento(transaccion.getId(), estado, origen, detalle);
    }

    private void registrarEvento(Long transaccionId, EstadoTransaccion estado, String origen, String detalle) {
        EventoTransaccion evento = new EventoTransaccion();
        evento.setTransaccionId(transaccionId);
        evento.setEstado(estado);
        evento.setOrigen(origen);
        evento.setDetalle(detalle);
        eventoTransaccionRepository.save(evento);
    }

    private BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
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
                    .atZone(ZoneId.systemDefault())
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