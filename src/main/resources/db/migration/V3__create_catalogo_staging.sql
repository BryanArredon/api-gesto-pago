CREATE TABLE productos_staging (
    id_servicio            INTEGER        NOT NULL,
    id_producto            INTEGER        NOT NULL,
    servicio               VARCHAR(256)   NOT NULL,
    producto               VARCHAR(256)   NOT NULL,
    id_cat_tipo_servicio   INTEGER        NOT NULL,
    tipo_front             SMALLINT       NOT NULL,
    tipo_referencia        VARCHAR(3)     NOT NULL,
    precio                 NUMERIC(12, 2) NOT NULL,
    legend                 TEXT,
    has_digito_verificador BOOLEAN        NOT NULL DEFAULT FALSE,
    show_ayuda             BOOLEAN        NOT NULL DEFAULT FALSE,
    cargado_en             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_staging_servicio_producto UNIQUE (id_servicio, id_producto)
);

CREATE TABLE catalogo_sincronizaciones (
    id                   BIGSERIAL PRIMARY KEY,
    version_id           BIGINT REFERENCES catalogo_versiones (id),
    estado               VARCHAR(20)  NOT NULL,
    inicio               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fin                  TIMESTAMPTZ,
    productos_recibidos  INTEGER      NOT NULL DEFAULT 0,
    productos_publicados INTEGER      NOT NULL DEFAULT 0,
    errores              INTEGER      NOT NULL DEFAULT 0,
    checksum             VARCHAR(64),
    mensaje              TEXT,
    CONSTRAINT ck_sync_estado CHECK (estado IN ('EN_PROCESO', 'EXITOSA', 'FALLIDA'))
);

CREATE INDEX idx_sync_inicio ON catalogo_sincronizaciones (inicio DESC);

CREATE VIEW v_catalogo_activo AS
SELECT p.*
FROM productos p
         JOIN catalogo_versiones v ON v.id = p.version_id
WHERE v.estado = 'ACTIVA'
  AND p.activo
  AND p.deleted_at IS NULL;
