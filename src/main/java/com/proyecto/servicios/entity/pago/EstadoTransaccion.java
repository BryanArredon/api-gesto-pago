package com.proyecto.servicios.entity.pago;

/**
 * Estado de vida de una transaccion de pago. Solo avanza por eventos
 * confirmados del proveedor o por el proceso interno de confirmacion.
 */
public enum EstadoTransaccion {
    PENDIENTE,
    EN_PROCESO,
    APROBADA,
    FALLIDA,
    RECHAZADA
}