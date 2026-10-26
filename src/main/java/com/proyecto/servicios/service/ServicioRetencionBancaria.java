package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetencionCuentaResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de dominio para la Etapa 2 y Etapa 4 de la Saga de pagos bancarios:
 * retencion atomica con PIN, liquidacion en firme, reverso compensatorio y escalamiento a revision.
 */
public interface ServicioRetencionBancaria {

    RetencionCuentaResponse retenerSaldo(
            String emailTitular,
            String numeroCuenta,
            String pinAutorizacion,
            BigDecimal monto,
            BigDecimal comision,
            String concepto,
            String idempotencyKey);

    void liquidarRetencion(String emailTitular, UUID folioMovimiento, String referenciaAutorizacion);

    /**
     * Ejecuta el reverso compensatorio con reintentos e idempotencia.
     *
     * @return {@code true} si el reverso quedo aplicado en {@code REVERSADO};
     *         {@code false} si fallo tras los reintentos y debe escalarse a {@code REQUIERE_REVISION}
     */
    boolean reversarRetencionConReintento(String emailTitular, UUID folioMovimiento, String motivo);

    void marcarParaRevision(String emailTitular, UUID folioMovimiento, String motivo);
}
