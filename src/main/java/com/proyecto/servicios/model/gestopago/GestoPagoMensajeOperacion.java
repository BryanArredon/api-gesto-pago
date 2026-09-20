package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

/**
 * Nodo MENSAJE de las respuestas de operacion del proveedor
 * (verifyReference.do, sendTx.do, confirmTx.do).
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoMensajeOperacion {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "REFERENCIA")
    private String referencia;

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "legend")
    private String legend;
}