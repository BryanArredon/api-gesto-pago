package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.CatalogoSyncResult;
import com.proyecto.servicios.service.CatalogoSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoSyncService catalogoSyncService;

    @PostMapping(value = "/catalogo/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoSyncResult> sincronizar() {
        CatalogoSyncResult resultado = catalogoSyncService.sincronizarCatalogo();
        HttpStatus status = "EXITOSA".equals(resultado.getEstado())
                ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return new ResponseEntity<>(resultado, status);
    }

    @GetMapping(value = "/catalogo/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CatalogoProductoCache>> productos() {
        return new ResponseEntity<>(catalogoSyncService.obtenerProductosActivos(), HttpStatus.OK);
    }
}