package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CatalogoProductoCache;

import java.util.Optional;

/**
 * Consulta del catalogo activo autoritativo almacenado por el backend.
 * El cliente nunca define precios: los montos se validan contra este catalogo.
 */
public interface CatalogoConsulta {

    Optional<CatalogoProductoCache> buscarActivo(Integer idServicio, Integer idProducto);
}