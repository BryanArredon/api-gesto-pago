-- =====================================================================================
-- V8: Bandeja de notificaciones por usuario y cola de envio de correo
-- =====================================================================================

CREATE TABLE notificaciones (
    id                  BIGSERIAL    PRIMARY KEY,
    usuario_id          BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    email               VARCHAR(150) NOT NULL,
    tipo                VARCHAR(30)  NOT NULL DEFAULT 'TRANSACCION',
    titulo              VARCHAR(160) NOT NULL,
    cuerpo              TEXT         NOT NULL,
    transaccion_id      BIGINT       REFERENCES transacciones (id) ON DELETE SET NULL,
    dedupe_key          VARCHAR(160) NOT NULL,
    datos               TEXT,
    leida_en            TIMESTAMPTZ,
    email_estado        VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    email_intentos      INTEGER      NOT NULL DEFAULT 0,
    email_ultimo_error  TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notificaciones_dedupe UNIQUE (usuario_id, dedupe_key),
    CONSTRAINT ck_notificacion_email_estado
        CHECK (email_estado IN ('PENDIENTE', 'ENVIANDO', 'ENVIADO', 'FALLIDO', 'OMITIDO'))
);

CREATE INDEX idx_notificaciones_usuario_fecha
    ON notificaciones (usuario_id, created_at DESC);

CREATE INDEX idx_notificaciones_no_leidas
    ON notificaciones (usuario_id, created_at DESC)
    WHERE leida_en IS NULL;

CREATE INDEX idx_notificaciones_email_pendiente
    ON notificaciones (email_estado, updated_at)
    WHERE email_estado IN ('PENDIENTE', 'FALLIDO');
