package com.proyecto.servicios.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogoProductoCache {

    private Integer idServicio;

    private Integer idProducto;

    private String servicio;

    private String producto;

    private Integer idCatTipoServicio;

    private Short tipoFront;

    private String tipoReferencia;

    private BigDecimal precio;

    private String legend;

    private Boolean hasDigitoVerificador;

    private Boolean showAyuda;
}