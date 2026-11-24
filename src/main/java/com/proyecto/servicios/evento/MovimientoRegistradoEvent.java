package com.proyecto.servicios.evento;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;

import java.math.BigDecimal;

public record MovimientoRegistradoEvent(
        Long transaccionId,
        Long usuarioId,
        EstadoTransaccion estado,
        String origen,
        String detalle,
        BigDecimal monto,
        BigDecimal comision,
        String servicio,
        String producto,
        String referencia,
        String codigoErrorGestoPago,
        String motivoRechazo,
        String detalleError) {

    public MovimientoRegistradoEvent(
            Long transaccionId,
            Long usuarioId,
            EstadoTransaccion estado,
            String origen,
            String detalle,
            BigDecimal monto,
            BigDecimal comision,
            String servicio,
            String producto,
            String referencia) {
        this(
                transaccionId,
                usuarioId,
                estado,
                origen,
                detalle,
                monto,
                comision,
                servicio,
                producto,
                referencia,
                null,
                null,
                null);
    }
}
