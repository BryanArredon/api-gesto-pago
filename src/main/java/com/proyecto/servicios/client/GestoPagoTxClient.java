package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Operaicones de transaccion contra GestoPago. Todos los cuerpos se envian
 * en formato form-url-encoded segun la especificacion del proveedor.
 */
@FeignClient(name = "gestoPagoTx", url = "${gestopago.auth.url}")
public interface GestoPagoTxClient {

    @PostMapping(value = "/sistema/service/verifyReference.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String verifyReference(@RequestHeader("Authorization") String authorization,
                           @RequestBody Map<String, String> body);

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(@RequestHeader("Authorization") String authorization,
                  @RequestBody Map<String, String> body);

    @PostMapping(value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirmTx(@RequestHeader("Authorization") String authorization,
                     @RequestBody Map<String, String> body);
}