package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProducto {

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "producto")
    private String descripcion;

    @XmlAttribute(name = "idServicio")
    private String idServicio;

    @XmlAttribute(name = "idProducto")
    private String idProducto;

    @XmlAttribute(name = "idCatTipoServicio")
    private String idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private String tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private String hasDigitoVerificador;

    @XmlAttribute(name = "precio")
    private String precio;

    @XmlAttribute(name = "showAyuda")
    private String showAyuda;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlElement(name = "legend")
    private String legend;
}