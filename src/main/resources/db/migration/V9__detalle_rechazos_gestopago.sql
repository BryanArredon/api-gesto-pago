-- =====================================================================================
-- V9: Almacenar codigo de error de GestoPago y motivo amigable del rechazo
-- =====================================================================================

ALTER TABLE transacciones
    ADD COLUMN IF NOT EXISTS codigo_error_gesto_pago VARCHAR(50),
    ADD COLUMN IF NOT EXISTS motivo_rechazo TEXT;

