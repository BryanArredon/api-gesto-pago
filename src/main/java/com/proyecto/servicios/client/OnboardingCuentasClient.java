package com.proyecto.servicios.client;

import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.ConsultaRetencionRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.LiquidarRetencionRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetencionCuentaResponse;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetenerSaldoRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.ReversarRetencionRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RevisionRetencionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente HTTP declarativo hacia el microservicio {@code onboarding-clientes} para el manejo
 * de retenciones bancarias (Holds), liquidaciones y reversos compensatorios.
 */
@FeignClient(name = "onboardingCuentas", url = "${onboarding.clientes.url:http://localhost:8082}")
public interface OnboardingCuentasClient {

    @PostMapping(value = "/cuentas/retenciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RetencionCuentaResponse retenerSaldo(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody RetenerSaldoRequest request);

    @PostMapping(value = "/cuentas/retenciones/liquidar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RetencionCuentaResponse liquidarRetencion(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody LiquidarRetencionRequest request);

    @PostMapping(value = "/cuentas/retenciones/reversar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RetencionCuentaResponse reversarRetencion(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody ReversarRetencionRequest request);

    @PostMapping(value = "/cuentas/retenciones/revision",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RetencionCuentaResponse marcarRevision(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody RevisionRetencionRequest request);

    @PostMapping(value = "/cuentas/retenciones/consulta",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RetencionCuentaResponse consultarRetencion(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody ConsultaRetencionRequest request);
}
