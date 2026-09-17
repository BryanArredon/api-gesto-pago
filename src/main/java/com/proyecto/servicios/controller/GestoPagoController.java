package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GestoPagoController {

    private final GestoPagoTokenService gestoPagoTokenService;

    @GetMapping(value = "/gestopago/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProducto>> obtenerProductos() {
        return new ResponseEntity<>(gestoPagoTokenService.obtenerProductos(), HttpStatus.OK);
    }
}