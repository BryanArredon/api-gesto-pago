package com.onboarding.clientes.cliente.domain.model;

/** Sexo registrado del cliente. Se persiste el codigo, nunca la etiqueta visible. */
public enum Sexo {

    MUJER('M'),
    HOMBRE('H'),
    OTRO('X');

    private final char codigoCurp;

    Sexo(char codigoCurp) {
        this.codigoCurp = codigoCurp;
    }

    /** @return caracter que ocupa la posicion 17 de la CURP */
    public char codigoCurp() {
        return codigoCurp;
    }
}
