package com.proyecto.servicios.entity.pago;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estado de vida homologado de una transaccion dentro de la Saga de pagos bancarios.
 */
public enum EstadoTransaccion {
    PENDIENTE,
    RETENIDO,
    EN_PROCESO,
    LIQUIDADO,
    REVERSANDO,
    REVERSADO,
    REQUIERE_REVISION;

    public boolean esFinal() {
        return this == LIQUIDADO || this == REVERSADO;
    }

    public boolean permiteReintento() {
        return this == REVERSADO;
    }

    public boolean permiteConfirmacion() {
        return this == EN_PROCESO || this == RETENIDO;
    }

    public boolean permiteTransicionarA(EstadoTransaccion destino) {
        if (this == destino) {
            return true;
        }
        Set<EstadoTransaccion> permitidos = switch (this) {
            case PENDIENTE -> EnumSet.of(RETENIDO, REVERSADO, REQUIERE_REVISION);
            case RETENIDO -> EnumSet.of(LIQUIDADO, EN_PROCESO, REVERSANDO, REVERSADO, REQUIERE_REVISION);
            case EN_PROCESO -> EnumSet.of(LIQUIDADO, REVERSANDO, REVERSADO, REQUIERE_REVISION);
            case REVERSANDO -> EnumSet.of(REVERSADO, REQUIERE_REVISION);
            case REQUIERE_REVISION -> EnumSet.of(LIQUIDADO, REVERSANDO, REVERSADO);
            case LIQUIDADO, REVERSADO -> EnumSet.of(PENDIENTE);
        };
        return permitidos.contains(destino);
    }
}
