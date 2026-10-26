-- =====================================================================================
-- V7: Saga de pagos bancarios con retenciones (Holds), hash de idempotencia y confirmacion escalonada
-- =====================================================================================

ALTER TABLE transacciones
    ADD COLUMN IF NOT EXISTS numero_cuenta VARCHAR(10),
    ADD COLUMN IF NOT EXISTS folio_retencion UUID,
    ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS intentos_confirmacion INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS proxima_confirmacion_en TIMESTAMPTZ;

-- Homologar estados existentes al vocabulario unificado de la Saga
UPDATE transacciones SET estado = 'LIQUIDADO' WHERE estado = 'APROBADA';
UPDATE transacciones SET estado = 'REVERSADO' WHERE estado IN ('FALLIDA', 'RECHAZADA');

UPDATE eventos_transaccion SET estado = 'LIQUIDADO' WHERE estado = 'APROBADA';
UPDATE eventos_transaccion SET estado = 'REVERSADO' WHERE estado IN ('FALLIDA', 'RECHAZADA');

ALTER TABLE transacciones DROP CONSTRAINT IF EXISTS ck_transaccion_estado;
ALTER TABLE transacciones ADD CONSTRAINT ck_transaccion_estado
    CHECK (estado IN ('PENDIENTE', 'RETENIDO', 'EN_PROCESO', 'LIQUIDADO', 'REVERSANDO', 'REVERSADO', 'REQUIERE_REVISION'));

CREATE INDEX IF NOT EXISTS idx_transacciones_confirmacion_pendiente
    ON transacciones (estado, proxima_confirmacion_en)
    WHERE estado IN ('EN_PROCESO', 'RETENIDO');
