package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Introspeccion de sesiones contra {@code onboarding-clientes}: responde 200 si la sesion del token sigue
 * vigente y 401 si fue revocada (logout, baja, acceso desde otro canal) o vencio por inactividad.
 */
@FeignClient(name = "onboardingSesion", url = "${onboarding.clientes.url:http://localhost:8082}")
public interface OnboardingSesionClient {

    @PostMapping(value = "/auth/sesion", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> consultarSesion(@RequestHeader("Authorization") String authorization);
}
