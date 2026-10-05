-- =====================================================================================
--  Onboarding de Clientes Personas Fisicas  ::  Esquema inicial
--  Decisiones de diseno (ver docs/adr):
--   * Claves primarias UUID v7 (generadas en la aplicacion): ordenables, no adivinables,
--     seguras al exponerlas en la API y sin coordinar rangos entre nodos.
--   * Todas las fechas de auditoria son TIMESTAMPTZ: el instante es un hecho absoluto,
--     la zona horaria es solo presentacion.
--   * Los importes son NUMERIC(18,2) (pesos mexicanos, 2 decimales) y jamas punto flotante.
--   * La integridad se refuerza en la base (CHECK, FK, indices unicos) y no solo en la app.
--   * snake_case, identificadores <= 63 bytes.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- Funciones de soporte
-- -------------------------------------------------------------------------------------

-- Secuencia de folios de cuenta. Se declara antes de las funciones que la consumen.
CREATE SEQUENCE cuenta_numero_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

-- Marca de actualizacion gestionada por la base: evita depender del reloj de la app.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- Digito verificador Luhn (mod 10) para el numero de cuenta.
CREATE OR REPLACE FUNCTION luhn_check_digit(digits text) RETURNS text
    LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    suma     integer := 0;
    factor   integer;
    digito   integer;
    idx      integer := length(digits) - 1;
    total    integer := length(digits);
BEGIN
    WHILE idx >= 0 LOOP
        digito := substr(digits, idx + 1, 1)::integer;
        -- Se duplica el digito en las posiciones impares contando desde la derecha de la base.
        factor := CASE WHEN (total - idx) % 2 = 1 THEN 2 ELSE 1 END;
        IF factor = 2 THEN
            digito := digito * 2;
            IF digito > 9 THEN
                digito := digito - 9;
            END IF;
        END IF;
        suma := suma + digito;
        idx := idx - 1;
    END LOOP;
    RETURN ((10 - (suma % 10)) % 10)::text;
END;
$$;

-- Numero de cuenta completo (9 digitos de secuencia + verificador Luhn) para cuando la aplicacion
-- necesita conocerlo antes de insertar (por ejemplo, para devolverlo en la respuesta de alta).
CREATE OR REPLACE FUNCTION cuenta_generar_numero() RETURNS char(10)
    LANGUAGE plpgsql VOLATILE AS $$
DECLARE
    base text := lpad(nextval('cuenta_numero_seq')::text, 9, '0');
BEGIN
    RETURN (base || luhn_check_digit(base))::char(10);
END;
$$;

-- Asigna el numero de cuenta si la aplicacion no lo provee: 9 digitos de secuencia + Luhn.
CREATE OR REPLACE FUNCTION cuenta_asignar_numero() RETURNS trigger
    LANGUAGE plpgsql AS $$
DECLARE
    base text;
BEGIN
    IF NEW.numero_cuenta IS NULL THEN
        base := lpad(nextval('cuenta_numero_seq')::text, 9, '0');
        NEW.numero_cuenta := base || luhn_check_digit(base);
    END IF;
    RETURN NEW;
END;
$$;

-- Invariante: "solo los clientes activos pueden tener cuentas activas".
CREATE OR REPLACE FUNCTION cuenta_validar_cliente_activo() RETURNS trigger
    LANGUAGE plpgsql AS $$
DECLARE
    cliente_activo boolean;
BEGIN
    SELECT c.activo INTO cliente_activo FROM cliente c WHERE c.id = NEW.cliente_id;

    IF cliente_activo IS NULL THEN
        RAISE EXCEPTION 'La cuenta % referencia un cliente inexistente', NEW.id
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF NEW.estatus = 'ACTIVA' AND NOT cliente_activo THEN
        RAISE EXCEPTION 'No es posible activar la cuenta % de un cliente inactivo', COALESCE(NEW.numero_cuenta, NEW.id::text)
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- Invariante: al desactivar un cliente, sus cuentas y su usuario se desactivan en cascada.
-- La aplicacion tambien lo hace de forma explicita; aqui esta la garantia del motor.
CREATE OR REPLACE FUNCTION cliente_cascada_baja() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.activo AND NOT NEW.activo THEN
        UPDATE cuenta SET estatus = 'INACTIVA', updated_at = now() WHERE cliente_id = NEW.id AND estatus = 'ACTIVA';
        UPDATE usuario SET activo = false, updated_at = now() WHERE cliente_id = NEW.id AND activo = true;
    END IF;
    RETURN NEW;
END;
$$;

-- -------------------------------------------------------------------------------------
-- Tablas
-- -------------------------------------------------------------------------------------

CREATE TABLE cliente (
    id                 uuid            PRIMARY KEY,
    nombre             varchar(50)     NOT NULL,
    segundo_nombre     varchar(50),
    apellido_paterno   varchar(50)     NOT NULL,
    apellido_materno   varchar(50)     NOT NULL,
    fecha_nacimiento   date            NOT NULL,
    curp               varchar(18)     NOT NULL,
    rfc                varchar(13)     NOT NULL,
    sexo               varchar(10)     NOT NULL,
    nacionalidad       varchar(50)     NOT NULL,
    estado_civil       varchar(20)     NOT NULL,
    correo_electronico varchar(100)    NOT NULL,
    telefono_movil     char(10)        NOT NULL,
    telefono_alterno   char(10),
    ocupacion          varchar(100)    NOT NULL,
    empresa            varchar(100),
    ingreso_mensual    numeric(18, 2)  NOT NULL,
    activo             boolean         NOT NULL DEFAULT true,
    fecha_baja         timestamptz,
    created_at         timestamptz     NOT NULL DEFAULT now(),
    updated_at         timestamptz     NOT NULL DEFAULT now(),

    CONSTRAINT uq_cliente_curp UNIQUE (curp),
    CONSTRAINT uq_cliente_rfc UNIQUE (rfc),

    CONSTRAINT ck_cliente_nombre_letras CHECK (nombre ~ '^[A-ZÁÉÍÓÚÜÑáéíóúüñ]+([[:space:]][A-ZÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    CONSTRAINT ck_cliente_segundo_nombre_letras CHECK (segundo_nombre IS NULL OR segundo_nombre ~ '^[A-ZÁÉÍÓÚÜÑáéíóúüñ]+([[:space:]][A-ZÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    CONSTRAINT ck_cliente_apellido_paterno_letras CHECK (apellido_paterno ~ '^[A-ZÁÉÍÓÚÜÑáéíóúüñ]+([[:space:]][A-ZÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    CONSTRAINT ck_cliente_apellido_materno_letras CHECK (apellido_materno ~ '^[A-ZÁÉÍÓÚÜÑáéíóúüñ]+([[:space:]][A-ZÁÉÍÓÚÜÑáéíóúüñ]+)*$'),
    CONSTRAINT ck_cliente_curp_formato CHECK (char_length(curp) = 18 AND curp ~ '^[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9HM][A-Z0-9]$'),
    CONSTRAINT ck_cliente_rfc_formato CHECK (rfc ~ '^[A-ZÑ&]{4}[0-9]{6}[0-9A-Z]{3}$'),
    CONSTRAINT ck_cliente_sexo CHECK (sexo IN ('MUJER', 'HOMBRE', 'OTRO')),
    CONSTRAINT ck_cliente_estado_civil CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE', 'SEPARADO', 'OTRO')),
    CONSTRAINT ck_cliente_correo_formato CHECK (correo_electronico ~* '^[^[:space:]@,;]+@[^[:space:]@,;]+\.[A-Za-z]{2,}$'),
    CONSTRAINT ck_cliente_telefono_movil CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_cliente_telefono_alterno CHECK (telefono_alterno IS NULL OR telefono_alterno ~ '^[0-9]{10}$'),
    CONSTRAINT ck_cliente_ingreso_positivo CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_cliente_nacimiento_no_futuro CHECK (fecha_nacimiento <= current_date),
    CONSTRAINT ck_cliente_baja_consistente CHECK ((activo AND fecha_baja IS NULL) OR (NOT activo AND fecha_baja IS NOT NULL))
);

COMMENT ON TABLE cliente IS 'Clientes persona fisica. La baja es logica: activo=false + fecha_baja.';

CREATE INDEX ix_cliente_activo ON cliente (activo) WHERE activo;
CREATE INDEX ix_cliente_registro ON cliente (created_at DESC);
CREATE INDEX ix_cliente_nacimiento ON cliente (fecha_nacimiento);
CREATE INDEX ix_cliente_nombre_completo ON cliente (lower(apellido_paterno), lower(apellido_materno), lower(nombre));

-- El indice unico de correo es sobre lower(correo): el login no distingue mayusculas.
CREATE UNIQUE INDEX ux_cliente_correo_lower ON cliente (lower(correo_electronico));

CREATE TABLE domicilio (
    id              uuid          PRIMARY KEY,
    cliente_id      uuid          NOT NULL,
    calle           varchar(100)  NOT NULL,
    numero_exterior varchar(10)   NOT NULL,
    numero_interior varchar(10),
    colonia         varchar(100)  NOT NULL,
    municipio       varchar(100)  NOT NULL,
    estado          varchar(50)   NOT NULL,
    codigo_postal   char(5)       NOT NULL,
    pais            varchar(50)   NOT NULL DEFAULT 'México',
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT fk_domicilio_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE CASCADE,
    CONSTRAINT uq_domicilio_cliente UNIQUE (cliente_id),
    CONSTRAINT ck_domicilio_codigo_postal CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT ck_domicilio_calle CHECK (char_length(calle) BETWEEN 3 AND 100),
    CONSTRAINT ck_domicilio_colonia CHECK (char_length(colonia) BETWEEN 2 AND 100)
);

COMMENT ON TABLE domicilio IS 'Relacion 1:1 con cliente (un cliente tiene un domicilio vigente).';

CREATE TABLE cuenta (
    id             uuid            PRIMARY KEY,
    cliente_id     uuid            NOT NULL,
    numero_cuenta  char(10)        NOT NULL,
    tipo_cuenta    varchar(20)     NOT NULL DEFAULT 'AHORRO',
    moneda         char(3)         NOT NULL DEFAULT 'MXN',
    saldo          numeric(18, 2)  NOT NULL DEFAULT 0,
    estatus        varchar(20)     NOT NULL DEFAULT 'ACTIVA',
    version        bigint          NOT NULL DEFAULT 0,
    created_at     timestamptz     NOT NULL DEFAULT now(),
    updated_at     timestamptz     NOT NULL DEFAULT now(),

    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT uq_cuenta_numero UNIQUE (numero_cuenta),
    CONSTRAINT uq_cuenta_cliente UNIQUE (cliente_id),
    CONSTRAINT ck_cuenta_tipo CHECK (tipo_cuenta IN ('AHORRO', 'CORRIENTE', 'NOMINA', 'INVERSION')),
    CONSTRAINT ck_cuenta_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA', 'CERRADA')),
    CONSTRAINT ck_cuenta_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT ck_cuenta_moneda CHECK (moneda ~ '^[A-Z]{3}$')
);

COMMENT ON TABLE cuenta IS 'Cuenta bancaria. Numero de 10 digitos: 9 de secuencia + 1 verificador Luhn.';

CREATE INDEX ix_cuenta_cliente ON cuenta (cliente_id);
CREATE INDEX ix_cuenta_estatus ON cuenta (estatus);
-- Indice parcial: el caso de uso caliente es "cuentas activas".
CREATE INDEX ix_cuenta_activas ON cuenta (cliente_id) WHERE estatus = 'ACTIVA';

CREATE TABLE usuario (
    id                  uuid          PRIMARY KEY,
    cliente_id          uuid          NOT NULL,
    correo_electronico  varchar(100)  NOT NULL,
    password_hash       varchar(100)  NOT NULL,
    pin_hash            varchar(100),
    activo              boolean       NOT NULL DEFAULT true,
    intentos_fallidos   integer       NOT NULL DEFAULT 0,
    bloqueado_hasta     timestamptz,
    ultimo_acceso       timestamptz,
    keycloak_sub        varchar(255),
    created_at          timestamptz   NOT NULL DEFAULT now(),
    updated_at          timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE CASCADE,
    CONSTRAINT uq_usuario_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuario_keycloak_sub UNIQUE (keycloak_sub),
    CONSTRAINT ck_usuario_password_hash CHECK (char_length(password_hash) = 60),
    CONSTRAINT ck_usuario_pin_hash CHECK (pin_hash IS NULL OR char_length(pin_hash) = 60),
    CONSTRAINT ck_usuario_intentos CHECK (intentos_fallidos >= 0)
);

COMMENT ON TABLE usuario IS 'Credenciales del cliente: BCrypt de contrasenia y PIN. Nunca se almacena en claro.';
COMMENT ON COLUMN usuario.password_hash IS 'Hash BCrypt (coste 12) de la contrasenia del cliente.';
COMMENT ON COLUMN usuario.pin_hash IS 'Hash BCrypt del PIN de accesobiometrico/de desbloqueo.';

CREATE UNIQUE INDEX ux_usuario_correo_lower ON usuario (lower(correo_electronico));
CREATE INDEX ix_usuario_activo ON usuario (activo) WHERE activo;

CREATE TABLE dispositivo_biometrico (
    id                uuid          PRIMARY KEY,
    usuario_id        uuid          NOT NULL,
    nombre            varchar(60)   NOT NULL,
    tipo              varchar(30)   NOT NULL,
    algoritmo         varchar(20)   NOT NULL DEFAULT 'SHA256withECDSA',
    public_key        text          NOT NULL,
    plantilla_id      varchar(128),
    activo            boolean       NOT NULL DEFAULT true,
    ultimo_uso        timestamptz,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uq_dispositivo_usuario_nombre UNIQUE (usuario_id, nombre),
    CONSTRAINT ck_dispositivo_tipo CHECK (tipo IN ('HUELLA_DIGITAL', 'ROSTRO_RECONOCIDO', 'LLAVE_SEGURIDAD')),
    CONSTRAINT ck_dispositivo_algoritmo CHECK (algoritmo IN ('SHA256withECDSA', 'SHA256withRSA'))
);

COMMENT ON TABLE dispositivo_biometrico IS
    'Llaves publicas enroladas por el cliente. La biometria protege una clave privada que nunca sale del dispositivo.';

CREATE INDEX ix_dispositivo_usuario ON dispositivo_biometrico (usuario_id) WHERE activo;

CREATE TABLE refresh_token (
    id            uuid         PRIMARY KEY,
    usuario_id    uuid         NOT NULL,
    familia_id    uuid         NOT NULL,
    token_hash    char(64)     NOT NULL,
    expira_en     timestamptz  NOT NULL,
    revocado     boolean      NOT NULL DEFAULT false,
    usado         boolean      NOT NULL DEFAULT false,
    dispositivo   varchar(120),
    ip_origen     inet,
    creado_en     timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

COMMENT ON TABLE refresh_token IS
    'Refresh tokens con rotacion: cada token se usa una vez y la reutilizacion revoca la familia completa.';

CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);
CREATE INDEX ix_refresh_token_familia ON refresh_token (familia_id);
CREATE INDEX ix_refresh_token_expira ON refresh_token (expira_en) WHERE NOT revocado;

CREATE TABLE desafio_biometrico (
    id             uuid         PRIMARY KEY,
    usuario_id     uuid         NOT NULL,
    nonce          char(64)     NOT NULL,
    expira_en      timestamptz  NOT NULL,
    consumido      boolean      NOT NULL DEFAULT false,
    intentos       integer      NOT NULL DEFAULT 0,
    creado_en      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT fk_desafio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);

COMMENT ON TABLE desafio_biometrico IS 'Desafios de un solo uso para la autenticacion por firma biometrica.';

CREATE INDEX ix_desafio_usuario ON desafio_biometrico (usuario_id) WHERE NOT consumido;

CREATE TABLE evento_dominio (
    id              uuid         PRIMARY KEY,
    tipo            varchar(80)  NOT NULL,
    agregado_tipo   varchar(40)  NOT NULL,
    agregado_id     uuid         NOT NULL,
    carga_util      jsonb        NOT NULL,
    correlation_id  varchar(64),
    actor           varchar(120),
    creado_en       timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE evento_dominio IS 'Auditoria append-only de los eventos de negocio relevantes.';

CREATE INDEX ix_evento_agregado ON evento_dominio (agregado_tipo, agregado_id, creado_en DESC);
CREATE INDEX ix_evento_creado ON evento_dominio (creado_en DESC);

-- -------------------------------------------------------------------------------------
-- Triggers
-- -------------------------------------------------------------------------------------

CREATE TRIGGER tg_cliente_updated_at BEFORE UPDATE ON cliente
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tg_domicilio_updated_at BEFORE UPDATE ON domicilio
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tg_cuenta_updated_at BEFORE UPDATE ON cuenta
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tg_usuario_updated_at BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER tg_dispositivo_updated_at BEFORE UPDATE ON dispositivo_biometrico
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER tg_cuenta_numero BEFORE INSERT ON cuenta
    FOR EACH ROW EXECUTE FUNCTION cuenta_asignar_numero();
CREATE TRIGGER tg_cuenta_cliente_activo BEFORE INSERT OR UPDATE ON cuenta
    FOR EACH ROW EXECUTE FUNCTION cuenta_validar_cliente_activo();
CREATE TRIGGER tg_cliente_cascada BEFORE UPDATE ON cliente
    FOR EACH ROW EXECUTE FUNCTION cliente_cascada_baja();

-- -------------------------------------------------------------------------------------
-- Vistas de lectura (desnormalizacion controlada para reportes)
-- -------------------------------------------------------------------------------------

CREATE VIEW v_cliente_cuenta AS
SELECT c.id            AS cliente_id,
       c.curp,
       c.rfc,
       c.correo_electronico,
       c.activo        AS cliente_activo,
       cu.id           AS cuenta_id,
       cu.numero_cuenta,
       cu.tipo_cuenta,
       cu.saldo,
       cu.estatus      AS cuenta_estatus,
       cu.created_at   AS cuenta_creada_en
FROM cliente c
         JOIN cuenta cu ON cu.cliente_id = c.id;
