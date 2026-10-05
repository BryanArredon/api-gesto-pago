package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.shared.error.DatoInvalidoException;
import java.util.List;
import java.util.Locale;

/**
 * Domicilio del cliente (relacion 1:1 con {@link Cliente}).
 *
 * <p>Entidad con invariantes propias: es un value object con identidad tecnica porque la base de datos la
 * referencia, pero conceptualmente no tiene comportamiento propio mas alla de su reemplazo. Al ser
 * inmutable, actualizar el domicilio significa crear uno nuevo y sustituirlo en el agregado, lo que evita
 * estados intermedios invalidos.
 */
public final class Domicilio {

    private static final int LONGITUD_MAXIMA = 100;

    private final String calle;
    private final String numeroExterior;
    private final String numeroInterior;
    private final String colonia;
    private final String municipio;
    private final String estado;
    private final CodigoPostal codigoPostal;
    private final String pais;

    public Domicilio(
            String calle,
            String numeroExterior,
            String numeroInterior,
            String colonia,
            String municipio,
            String estado,
            CodigoPostal codigoPostal,
            String pais) {
        this.calle = textoObligatorio(calle, "domicilio.calle", LONGITUD_MAXIMA);
        this.numeroExterior = textoObligatorio(numeroExterior, "domicilio.numeroExterior", 10);
        this.numeroInterior = textoOpcional(numeroInterior, "domicilio.numeroInterior", 10);
        this.colonia = textoObligatorio(colonia, "domicilio.colonia", LONGITUD_MAXIMA);
        this.municipio = textoObligatorio(municipio, "domicilio.municipio", LONGITUD_MAXIMA);
        this.estado = textoObligatorio(estado, "domicilio.estado", 50);
        if (codigoPostal == null) {
            throw new DatoInvalidoException("El codigo postal es obligatorio", List.of("domicilio.codigoPostal: es obligatorio"));
        }
        this.codigoPostal = codigoPostal;
        this.pais = pais == null || pais.isBlank() ? "México" : textoObligatorio(pais, "domicilio.pais", LONGITUD_MAXIMA);
    }

    public static Domicilio de(
            String calle,
            String numeroExterior,
            String numeroInterior,
            String colonia,
            String municipio,
            String estado,
            CodigoPostal codigoPostal,
            String pais) {
        return new Domicilio(calle, numeroExterior, numeroInterior, colonia, municipio, estado, codigoPostal, pais);
    }

    public String calle() {
        return calle;
    }

    public String numeroExterior() {
        return numeroExterior;
    }

    public String numeroInterior() {
        return numeroInterior;
    }

    public String colonia() {
        return colonia;
    }

    public String municipio() {
        return municipio;
    }

    public String estado() {
        return estado;
    }

    public CodigoPostal codigoPostal() {
        return codigoPostal;
    }

    public String pais() {
        return pais;
    }

    private static String textoObligatorio(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El campo " + campo + " es obligatorio", List.of(campo + ": es obligatorio"));
        }
        String normalizado = valor.trim().replaceAll("\\s+", " ");
        if (normalizado.length() > longitudMaxima) {
            throw new DatoInvalidoException(
                    "El campo " + campo + " excede " + longitudMaxima + " caracteres",
                    List.of(campo + ": maximo " + longitudMaxima + " caracteres"));
        }
        return normalizado;
    }

    private static String textoOpcional(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return textoObligatorio(valor, campo, longitudMaxima);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s, %s, %s, %s %s", calle, numeroExterior, colonia, municipio, estado, codigoPostal);
    }
}
