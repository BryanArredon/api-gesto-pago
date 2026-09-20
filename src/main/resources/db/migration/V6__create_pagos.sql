CREATE TABLE transacciones (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT         NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    id_servicio         INTEGER        NOT NULL,
    id_producto         INTEGER        NOT NULL,
    servicio            VARCHAR(256)   NOT NULL,
    producto            VARCHAR(256)   NOT NULL,
    referencia          VARCHAR(100)   NOT NULL,
    monto               NUMERIC(12, 2) NOT NULL,
    comision            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado              VARCHAR(20)    NOT NULL,
    idempotency_key     VARCHAR(36)    NOT NULL,
    upc                 VARCHAR(15),
    unidad              VARCHAR(25),
    numero_autorizacion VARCHAR(60),
    id_tx               VARCHAR(40),
    pin                 TEXT,
    legend              TEXT,
    error_mensaje       TEXT,
    fecha               TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transacciones_reintento UNIQUE (usuario_id, idempotency_key),
    CONSTRAINT ck_transaccion_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'APROBADA', 'FALLIDA', 'RECHAZADA')),
    CONSTRAINT ck_transaccion_monto CHECK (monto > 0)
);

CREATE INDEX idx_transacciones_usuario_fecha ON transacciones (usuario_id, created_at DESC);
CREATE INDEX idx_transacciones_estado ON transacciones (estado);

CREATE TABLE eventos_transaccion (
    id             BIGSERIAL   PRIMARY KEY,
    transaccion_id BIGINT      NOT NULL REFERENCES transacciones (id) ON DELETE CASCADE,
    estado         VARCHAR(20) NOT NULL,
    origen         VARCHAR(30) NOT NULL,
    detalle        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_eventos_transaccion ON eventos_transaccion (transaccion_id, created_at);