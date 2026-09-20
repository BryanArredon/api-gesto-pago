package com.proyecto.servicios.model.pago;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado de la verificacion de una referencia contra el proveedor.
 * El monto autoritativo lo devuelve el proveedor; el cliente solo lo muestra.
 */
@Getter
@Setter
@NoArgsConstructor
public class VerificarReferenciaResponse {

    /** 0 cuando la referencia es valida; cualquier otro valor indica invalidez. */
    private Integer codigo;

    private Boolean valida;

    /** Monto autoritativo (saldo) devuelto por el proveedor, si aplica. */
    private String monto;

    private String mensaje;

    public static VerificarReferenciaResponse valida(String monto, String mensaje) {
        VerificarReferenciaResponse r = new VerificarReferenciaResponse();
        r.setCodigo(0);
        r.setValida(true);
        r.setMonto(monto);
        r.setMensaje(mensaje);
        return r;
    }

    public static VerificarReferenciaResponse invalida(String mensaje) {
        VerificarReferenciaResponse r = new VerificarReferenciaResponse();
        r.setCodigo(1);
        r.setValida(false);
        r.setMensaje(mensaje);
        return r;
    }
}