package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProducto", url = "${gestopago.auth.url}")
public interface GestoPagoProductoClient {

    @GetMapping("/sistema/service/getProductList.do")
    String getProductList(@RequestHeader("Authorization") String authorization);
}