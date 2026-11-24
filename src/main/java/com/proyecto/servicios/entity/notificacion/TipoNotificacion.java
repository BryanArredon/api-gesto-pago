package com.proyecto.servicios.entity.notificacion;

public enum TipoNotificacion {
    TRANSACCION,
    SEGURIDAD,
    CUENTA,
    SISTEMA;

    public static TipoNotificacion desde(String valor) {
        if (valor == null) {
            return SISTEMA;
        }
        for (TipoNotificacion tipo : values()) {
            if (tipo.name().equalsIgnoreCase(valor.trim())) {
                return tipo;
            }
        }
        return SISTEMA;
    }
}
