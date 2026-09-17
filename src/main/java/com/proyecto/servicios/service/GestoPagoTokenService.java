package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;

import java.util.List;
import java.util.Optional;

public interface GestoPagoTokenService {

    void renovarToken();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);

    List<GestoPagoProducto> obtenerProductos();
}
