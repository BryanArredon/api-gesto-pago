CREATE TABLE IF NO EXISTS gestor_pagos.productos(
    id SERIAL PRIMARY KEY,
    producto VARCHAR(100) NOT NULL,
    servicio VARCHAR(100) NOT NULL,
    id_servicio INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    id_cat_tipo_servicio INTEGER NOT NULL,
    tipo_front INTEGER,
    has_digito_verificador BOOLEAN NOT NULL,
    tipo_referencia VARCHAR(3) NOT NULL,
    precio VARCHAR(15) NOT NULL,
    show_ayuda BOOLEAN
);