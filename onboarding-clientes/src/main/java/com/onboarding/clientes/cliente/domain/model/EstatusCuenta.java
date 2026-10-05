package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;

/**
 * Estatus del ciclo de vida de una cuenta.
 *
 * <p>Transiciones validas:
 *
 * <pre>
 *   ACTIVA --&gt; INACTIVA   (baja del cliente, cierre)
 *   ACTIVA --&gt; BLOQUEADA  (suspension por fraude o por el cliente)
 *   ACTIVA --&gt; CERRADA    (cierre definitivo, sin retorno)
 *   INACTIVA --&gt; CERRADA
 *   BLOQUEADA --&gt; ACTIVA  (desbloqueo)
 * </pre>
 */
public enum EstatusCuenta {

    ACTIVA(true),
    INACTIVA(false),
    BLOQUEADA(false),
    CERRADA(false);

    private final boolean admiteOperaciones;

    EstatusCuenta(boolean admiteOperaciones) {
        this.admiteOperaciones = admiteOperaciones;
    }

    public boolean admiteOperaciones() {
        return admiteOperaciones;
    }

    /** @return {@code true} si la transicion al estatus indicado esta permitida */
    public boolean puedeTransicionarA(EstatusCuenta destino) {
        return switch (this) {
            case ACTIVA -> destino == INACTIVA || destino == BLOQUEADA || destino == CERRADA;
            case INACTIVA, BLOQUEADA -> destino == CERRADA || destino == ACTIVA || destino == INACTIVA;
            case CERRADA -> false;
        };
    }

    public EstatusCuenta transicionarA(EstatusCuenta destino) {
        if (!puedeTransicionarA(destino)) {
            throw new ErrorNegocio(
                    CodigoError.CUENTA_SIN_CLIENTE_ACTIVO,
                    "Transicion de estatus no permitida: " + this + " -> " + destino);
        }
        return destino;
    }
}
