package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import com.proyecto.servicios.repositorys.pago.TransaccionRepository;
import com.proyecto.servicios.service.PagosService;
import com.proyecto.servicios.util.ValidadorLuhn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Job transversal de la Saga de pagos bancarios:
 * <ul>
 *   <li>Confirmacion escalonada ({@code 62s}, {@code 3min}, {@code 10min}) de transacciones en {@code EN_PROCESO}.
 *   <li>Resolucion de holds huerfanos en {@code RETENIDO} cuyo TTL vencio sin respuesta del switch,
 *       consultando siempre el estado real del proveedor ({@code confirmTx.do}) antes de liquidar,
 *       liberar con reverso compensatorio o escalar a {@code REQUIERE_REVISION}.
 * </ul>
 */
@Component
@Slf4j
public class ConciliacionPagosScheduler {

    private final TransaccionRepository transaccionRepository;
    private final PagosService pagosService;

    public ConciliacionPagosScheduler(
            TransaccionRepository transaccionRepository,
            PagosService pagosService) {
        this.transaccionRepository = transaccionRepository;
        this.pagosService = pagosService;
    }

    @Scheduled(fixedDelayString = "${gestopago.conciliacion.interval-ms:30000}")
    public void conciliarPendientesYHoldsHuerfanos() {
        Instant ahora = Instant.now();
        List<Transaccion> candidatas = transaccionRepository
                .findByEstadoInAndProximaConfirmacionEnLessThanEqualOrderByProximaConfirmacionEnAsc(
                        EnumSet.of(EstadoTransaccion.EN_PROCESO, EstadoTransaccion.RETENIDO),
                        ahora);

        for (Transaccion tx : candidatas) {
            try {
                log.info(
                        "Conciliando transaccion id={} estado={} cuenta={} intento={}",
                        tx.getId(),
                        tx.getEstado(),
                        ValidadorLuhn.enmascararCuenta(tx.getNumeroCuenta()),
                        tx.getIntentosConfirmacion() + 1);
                pagosService.conciliarTransaccionPendiente(tx);
            } catch (Exception e) {
                log.error(
                        "Error al conciliar transaccion id={} cuenta={}",
                        tx.getId(),
                        ValidadorLuhn.enmascararCuenta(tx.getNumeroCuenta()),
                        e);
            }
        }
    }
}
