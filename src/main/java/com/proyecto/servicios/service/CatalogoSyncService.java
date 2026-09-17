package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.CatalogoSyncResult;

import java.util.List;

public interface CatalogoSyncService {

    CatalogoSyncResult sincronizarCatalogo();

    List<CatalogoProductoCache> obtenerProductosActivos();
}