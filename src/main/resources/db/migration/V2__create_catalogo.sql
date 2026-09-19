CREATE TABLE catalogo_versiones (
    id              BIGSERIAL PRIMARY KEY,
    version         INTEGER      NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    total_productos INTEGER      NOT NULL DEFAULT 0,
    checksum        VARCHAR(64),
    fecha_inicio    TIMESTAMPTZ,
    fecha_fin       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_catalogo_version UNIQUE (version),
    CONSTRAINT ck_catalogo_estado CHECK (estado IN ('EN_CARGA', 'ACTIVA', 'ARCHIVADA', 'FALLIDA'))
);

CREATE UNIQUE INDEX uq_catalogo_activa ON catalogo_versiones (estado) WHERE estado = 'ACTIVA';

CREATE TABLE productos (
    id                     BIGSERIAL PRIMARY KEY,
    version_id             BIGINT        NOT NULL REFERENCES catalogo_versiones (id),
    id_servicio            INTEGER       NOT NULL,
    id_producto            INTEGER       NOT NULL,
    servicio               VARCHAR(256)  NOT NULL,
    producto               VARCHAR(256)  NOT NULL,
    id_cat_tipo_servicio   INTEGER       NOT NULL,
    tipo_front             SMALLINT      NOT NULL,
    tipo_referencia        VARCHAR(3)    NOT NULL,
    precio                 NUMERIC(12, 2) NOT NULL,
    legend                 TEXT,
    has_digito_verificador BOOLEAN       NOT NULL DEFAULT FALSE,
    show_ayuda             BOOLEAN       NOT NULL DEFAULT FALSE,
    activo                 BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ,
    CONSTRAINT uq_productos_version_servicio_producto UNIQUE (version_id, id_servicio, id_producto),
    CONSTRAINT ck_productos_tipo_referencia CHECK (tipo_referencia IN ('a', 'b', 'c', 'ab', 'bc'))
);

CREATE INDEX idx_productos_version_categoria ON productos (version_id, id_cat_tipo_servicio);
CREATE INDEX idx_productos_version_servicio ON productos (version_id, id_servicio);
CREATE INDEX idx_productos_version_tipofront ON productos (version_id, tipo_front);
CREATE INDEX idx_productos_activo ON productos (version_id) WHERE activo AND deleted_at IS NULL;
