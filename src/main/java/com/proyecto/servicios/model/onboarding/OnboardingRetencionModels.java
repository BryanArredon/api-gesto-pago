package com.proyecto.servicios.model.onboarding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Contratos inmutables (records) para la comunicacion con el microservicio onboarding-clientes.
 */
public final class OnboardingRetencionModels {

    private OnboardingRetencionModels() {
    }

    public record RetenerSaldoRequest(
            String numeroCuenta,
            String pinAutorizacion,
            BigDecimal monto,
            BigDecimal comision,
            String moneda,
            String concepto,
            String idempotencyKey) {

        @Override
        public String toString() {
            return "RetenerSaldoRequest[numeroCuenta=******, pinAutorizacion=****, monto="
                    + monto + ", comision=" + comision + ", idempotencyKey=" + idempotencyKey + "]";
        }
    }

    public record LiquidarRetencionRequest(
            UUID folioMovimiento,
            String referenciaAutorizacion) {
    }

    public record ReversarRetencionRequest(
            UUID folioMovimiento,
            String motivo) {
    }

    public record RevisionRetencionRequest(
            UUID folioMovimiento,
            String motivo) {
    }

    public record ConsultaRetencionRequest(
            UUID folioMovimiento,
            String numeroCuenta,
            String idempotencyKey) {
    }

    public record RetencionCuentaResponse(
            UUID folioMovimiento,
            UUID cuentaId,
            UUID clienteId,
            String numeroCuenta,
            String idempotencyKey,
            BigDecimal monto,
            BigDecimal comision,
            BigDecimal montoTotal,
            String moneda,
            String concepto,
            String estado,
            String referenciaExterna,
            String motivoReverso,
            Instant expiraEn,
            Instant createdAt,
            Instant updatedAt) {
    }
}
