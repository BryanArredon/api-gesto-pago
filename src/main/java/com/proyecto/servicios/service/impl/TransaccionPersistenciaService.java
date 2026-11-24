package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.EventoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.evento.MovimientoRegistradoEvent;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.idempotencia.ClaveIdempotencia;
import com.proyecto.servicios.repositorys.pago.EventoTransaccionRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionRepository;
import com.proyecto.servicios.service.BloqueoIdempotencia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona las transiciones de estado y persistencia en transacciones de base de datos cortas.
 *
 * <p>Permite cumplir la regla bancaria de no mantener transacciones de base de datos abiertas mientras
 * se realizan llamadas HTTP externas hacia {@code onboarding-clientes} o {@code GestoPago}.
 */
@Service
public class TransaccionPersistenciaService {

    private final TransaccionRepository transaccionRepository;
    private final EventoTransaccionRepository eventoTransaccionRepository;
    private final BloqueoIdempotencia bloqueoIdempotencia;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    public TransaccionPersistenciaService(
            TransaccionRepository transaccionRepository,
            EventoTransaccionRepository eventoTransaccionRepository,
            BloqueoIdempotencia bloqueoIdempotencia) {
        this.transaccionRepository = transaccionRepository;
        this.eventoTransaccionRepository = eventoTransaccionRepository;
        this.bloqueoIdempotencia = bloqueoIdempotencia;
    }

    public record ResultadoPreparacion(Transaccion transaccion, boolean yaProcesada) {
    }

    @Transactional
    public ResultadoPreparacion iniciarIntencionPendiente(
            Long usuarioId,
            String numeroCuenta,
            CatalogoProductoCache producto,
            String referencia,
            BigDecimal monto,
            BigDecimal comision,
            String idempotencyKey,
            String payloadHash,
            String unidad) {
        ClaveIdempotencia clave = new ClaveIdempotencia(usuarioId, idempotencyKey);
        bloqueoIdempotencia.adquirir(clave);

        Optional<Transaccion> existenteOpt =
                transaccionRepository.findByUsuarioIdAndIdempotencyKey(usuarioId, idempotencyKey);
        if (existenteOpt.isPresent()) {
            Transaccion existente = existenteOpt.get();
            if (existente.getPayloadHash() != null && !existente.getPayloadHash().equals(payloadHash)) {
                throw ApiException.conflictoIdempotencia();
            }
            if (!existente.getEstado().permiteReintento()) {
                return new ResultadoPreparacion(existente, true);
            }
        }

        Transaccion tx = existenteOpt.orElseGet(Transaccion::new);
        if (tx.getId() == null) {
            tx.setUsuarioId(usuarioId);
            tx.setIdServicio(producto.getIdServicio());
            tx.setIdProducto(producto.getIdProducto());
            tx.setServicio(producto.getServicio());
            tx.setProducto(producto.getProducto());
            tx.setIdempotencyKey(idempotencyKey);
            tx.setUpc(derivarUpc(idempotencyKey));
            tx.setUnidad(unidad);
        }
        tx.setNumeroCuenta(numeroCuenta);
        tx.setPayloadHash(payloadHash);
        tx.setReferencia(referencia);
        tx.setMonto(monto);
        tx.setComision(comision);
        tx.setEstado(EstadoTransaccion.PENDIENTE);
        tx.setErrorMensaje(null);
        tx.setCodigoErrorGestoPago(null);
        tx.setMotivoRechazo(null);
        tx.setIntentosConfirmacion(0);
        tx.setProximaConfirmacionEn(null);

        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(guardada, EstadoTransaccion.PENDIENTE, "API",
                "Intencion de pago validada y registrada en estado PENDIENTE");
        return new ResultadoPreparacion(guardada, false);
    }

    @Transactional
    public Transaccion confirmarRetencionBancaria(Long transaccionId, UUID folioRetencion, Instant expiraEn) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setFolioRetencion(folioRetencion);
        tx.setEstado(EstadoTransaccion.RETENIDO);
        tx.setProximaConfirmacionEn(expiraEn);
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(
                guardada,
                EstadoTransaccion.RETENIDO,
                "ONBOARDING_BANCO",
                "Saldo retenido atomicamente con folio " + folioRetencion);
        return guardada;
    }

    @Transactional
    public Transaccion marcarLiquidada(
            Long transaccionId,
            String numAutorizacion,
            String idTx,
            String pin,
            String legend,
            BigDecimal montoProveedor,
            BigDecimal comisionProveedor,
            Instant fechaProveedor,
            String origen,
            String detalle) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setNumeroAutorizacion(numAutorizacion);
        tx.setIdTx(idTx);
        tx.setPin(pin);
        tx.setLegend(legend);
        if (montoProveedor != null) {
            tx.setMonto(montoProveedor);
        }
        if (comisionProveedor != null) {
            tx.setComision(comisionProveedor);
        }
        if (fechaProveedor != null) {
            tx.setFecha(fechaProveedor);
        }
        tx.setErrorMensaje(null);
        tx.setCodigoErrorGestoPago(null);
        tx.setMotivoRechazo(null);
        tx.setProximaConfirmacionEn(null);
        tx.setEstado(EstadoTransaccion.LIQUIDADO);
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(guardada, EstadoTransaccion.LIQUIDADO, origen, detalle);
        return guardada;
    }

    @Transactional
    public Transaccion programarConfirmacionEnProceso(
            Long transaccionId,
            String mensajeIncertidumbre,
            int intentosRealizados,
            Instant proximaConfirmacionEn,
            String origen) {
        return programarConfirmacionEnProceso(
                transaccionId, null, mensajeIncertidumbre, mensajeIncertidumbre, intentosRealizados, proximaConfirmacionEn, origen);
    }

    @Transactional
    public Transaccion programarConfirmacionEnProceso(
            Long transaccionId,
            String codigoErrorGestoPago,
            String motivoRechazo,
            String detalleError,
            int intentosRealizados,
            Instant proximaConfirmacionEn,
            String origen) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setEstado(EstadoTransaccion.EN_PROCESO);
        tx.setErrorMensaje(detalleError != null ? detalleError : motivoRechazo);
        if (codigoErrorGestoPago != null) {
            tx.setCodigoErrorGestoPago(codigoErrorGestoPago);
        }
        if (motivoRechazo != null) {
            tx.setMotivoRechazo(motivoRechazo);
        }
        tx.setIntentosConfirmacion(intentosRealizados);
        tx.setProximaConfirmacionEn(proximaConfirmacionEn);
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(
                guardada,
                EstadoTransaccion.EN_PROCESO,
                origen,
                "Resultado incierto (saldo protegido en RETENIDO); confirmTx programado para " + proximaConfirmacionEn);
        return guardada;
    }

    @Transactional
    public Transaccion iniciarReverso(Long transaccionId, String motivo, String origen) {
        return iniciarReverso(transaccionId, null, motivo, motivo, origen);
    }

    @Transactional
    public Transaccion iniciarReverso(
            Long transaccionId,
            String codigoErrorGestoPago,
            String motivoRechazo,
            String detalleError,
            String origen) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setEstado(EstadoTransaccion.REVERSANDO);
        tx.setErrorMensaje(detalleError != null ? detalleError : motivoRechazo);
        if (codigoErrorGestoPago != null) {
            tx.setCodigoErrorGestoPago(codigoErrorGestoPago);
        }
        if (motivoRechazo != null) {
            tx.setMotivoRechazo(motivoRechazo);
        }
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(guardada, EstadoTransaccion.REVERSANDO, origen, detalleError != null ? detalleError : motivoRechazo);
        return guardada;
    }

    @Transactional
    public Transaccion completarReverso(Long transaccionId, String motivo, String origen) {
        return completarReverso(transaccionId, null, motivo, motivo, origen);
    }

    @Transactional
    public Transaccion completarReverso(
            Long transaccionId,
            String codigoErrorGestoPago,
            String motivoRechazo,
            String detalleError,
            String origen) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setEstado(EstadoTransaccion.REVERSADO);
        tx.setErrorMensaje(detalleError != null ? detalleError : motivoRechazo);
        if (codigoErrorGestoPago != null) {
            tx.setCodigoErrorGestoPago(codigoErrorGestoPago);
        }
        if (motivoRechazo != null) {
            tx.setMotivoRechazo(motivoRechazo);
        }
        tx.setProximaConfirmacionEn(null);
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(
                guardada,
                EstadoTransaccion.REVERSADO,
                origen,
                "Reverso compensatorio aplicado en cuenta bancaria: " + (motivoRechazo != null ? motivoRechazo : detalleError));
        return guardada;
    }

    @Transactional
    public Transaccion marcarRequiereRevision(Long transaccionId, String motivo, String origen) {
        return marcarRequiereRevision(transaccionId, null, motivo, motivo, origen);
    }

    @Transactional
    public Transaccion marcarRequiereRevision(
            Long transaccionId,
            String codigoErrorGestoPago,
            String motivoRechazo,
            String detalleError,
            String origen) {
        Transaccion tx = obtenerOExcepcion(transaccionId);
        tx.setEstado(EstadoTransaccion.REQUIERE_REVISION);
        tx.setErrorMensaje(detalleError != null ? detalleError : motivoRechazo);
        if (codigoErrorGestoPago != null) {
            tx.setCodigoErrorGestoPago(codigoErrorGestoPago);
        }
        if (motivoRechazo != null) {
            tx.setMotivoRechazo(motivoRechazo);
        }
        tx.setProximaConfirmacionEn(null);
        Transaccion guardada = transaccionRepository.save(tx);
        registrarEvento(guardada, EstadoTransaccion.REQUIERE_REVISION, origen, detalleError != null ? detalleError : motivoRechazo);
        return guardada;
    }

    private Transaccion obtenerOExcepcion(Long id) {
        return transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Transaccion inexistente id=" + id));
    }

    private void registrarEvento(Transaccion transaccion, EstadoTransaccion estado, String origen, String detalle) {
        EventoTransaccion evento = new EventoTransaccion();
        evento.setTransaccionId(transaccion.getId());
        evento.setEstado(estado);
        evento.setOrigen(origen);
        evento.setDetalle(detalle);
        eventoTransaccionRepository.save(evento);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new MovimientoRegistradoEvent(
                    transaccion.getId(),
                    transaccion.getUsuarioId(),
                    estado,
                    origen,
                    detalle,
                    transaccion.getMonto(),
                    transaccion.getComision(),
                    transaccion.getServicio(),
                    transaccion.getProducto(),
                    transaccion.getReferencia(),
                    transaccion.getCodigoErrorGestoPago(),
                    transaccion.getMotivoRechazo(),
                    transaccion.getErrorMensaje()));
        }
    }

    public static String derivarUpc(String idempotencyKey) {
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
}
