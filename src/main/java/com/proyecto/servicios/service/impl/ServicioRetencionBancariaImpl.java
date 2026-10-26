package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.OnboardingCuentasClient;
import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.LiquidarRetencionRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetencionCuentaResponse;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RetenerSaldoRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.ReversarRetencionRequest;
import com.proyecto.servicios.model.onboarding.OnboardingRetencionModels.RevisionRetencionRequest;
import com.proyecto.servicios.service.ServicioRetencionBancaria;
import com.proyecto.servicios.util.ValidadorLuhn;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adaptador que coordina las operaciones bancarias contra {@code onboarding-clientes}
 * con traduccion determinista de errores RFC 9457 y reintentos idempotentes en reversos.
 */
@Service
@Slf4j
public class ServicioRetencionBancariaImpl implements ServicioRetencionBancaria {

    private static final int MAX_REINTENTOS_REVERSO = 3;
    private static final String MONEDA_MXN = "MXN";

    private final OnboardingCuentasClient onboardingClient;
    private final JwtService jwtService;

    public ServicioRetencionBancariaImpl(OnboardingCuentasClient onboardingClient, JwtService jwtService) {
        this.onboardingClient = onboardingClient;
        this.jwtService = jwtService;
    }

    @Override
    public RetencionCuentaResponse retenerSaldo(
            String emailTitular,
            String numeroCuenta,
            String pinAutorizacion,
            BigDecimal monto,
            BigDecimal comision,
            String concepto,
            String idempotencyKey) {
        String bearer = bearerHomologado(emailTitular, "CLIENTE");
        String traceId = MDC.get("traceId");
        RetenerSaldoRequest request = new RetenerSaldoRequest(
                numeroCuenta,
                pinAutorizacion,
                monto,
                comision,
                MONEDA_MXN,
                concepto,
                idempotencyKey);
        try {
            return onboardingClient.retenerSaldo(bearer, traceId, request);
        } catch (FeignException e) {
            throw traducirErrorOnboarding(e, numeroCuenta);
        }
    }

    @Override
    public void liquidarRetencion(String emailTitular, UUID folioMovimiento, String referenciaAutorizacion) {
        if (folioMovimiento == null) {
            return;
        }
        String bearer = bearerParaOperacionPosterior(emailTitular);
        String traceId = MDC.get("traceId");
        try {
            onboardingClient.liquidarRetencion(
                    bearer,
                    traceId,
                    new LiquidarRetencionRequest(folioMovimiento, referenciaAutorizacion));
        } catch (FeignException e) {
            log.error(
                    "Error al liquidar retencion bancaria folio={} status={} traceId={}",
                    folioMovimiento,
                    e.status(),
                    traceId,
                    e);
            throw ApiException.servicioBancarioNoDisponible(e);
        }
    }

    @Override
    public boolean reversarRetencionConReintento(String emailTitular, UUID folioMovimiento, String motivo) {
        if (folioMovimiento == null) {
            return true;
        }
        String bearer = bearerParaOperacionPosterior(emailTitular);
        String traceId = MDC.get("traceId");
        ReversarRetencionRequest request = new ReversarRetencionRequest(
                folioMovimiento,
                motivo == null || motivo.isBlank() ? "Reverso por rechazo de operacion" : motivo);

        for (int intento = 1; intento <= MAX_REINTENTOS_REVERSO; intento++) {
            try {
                onboardingClient.reversarRetencion(bearer, traceId, request);
                return true;
            } catch (FeignException e) {
                log.warn(
                        "Intento {}/{} de reverso bancario fallo para folio={} status={} traceId={}",
                        intento,
                        MAX_REINTENTOS_REVERSO,
                        folioMovimiento,
                        e.status(),
                        traceId);
            }
        }
        log.error(
                "ALERTA DLQ: Reverso bancario agotado tras {} intentos para folio={} traceId={}. Pasa a REQUIERE_REVISION",
                MAX_REINTENTOS_REVERSO,
                folioMovimiento,
                traceId);
        return false;
    }

    @Override
    public void marcarParaRevision(String emailTitular, UUID folioMovimiento, String motivo) {
        if (folioMovimiento == null) {
            return;
        }
        String bearer = bearerParaOperacionPosterior(emailTitular);
        String traceId = MDC.get("traceId");
        try {
            onboardingClient.marcarRevision(
                    bearer,
                    traceId,
                    new RevisionRetencionRequest(
                            folioMovimiento,
                            motivo == null || motivo.isBlank() ? "Requiere revision operativa" : motivo));
        } catch (FeignException e) {
            log.error(
                    "No se pudo notificar REQUIERE_REVISION en onboarding-clientes para folio={} status={} traceId={}",
                    folioMovimiento,
                    e.status(),
                    traceId);
        }
    }

    private String bearerHomologado(String email, String rol) {
        return "Bearer " + jwtService.generarTokenHomologado(email, rol);
    }

    private String bearerParaOperacionPosterior(String emailTitular) {
        if (emailTitular != null && !emailTitular.isBlank()) {
            return bearerHomologado(emailTitular, "CLIENTE");
        }
        return bearerHomologado("sistema@gestopago.interno", "ADMIN");
    }

    private ApiException traducirErrorOnboarding(FeignException e, String numeroCuenta) {
        String cuerpo = e.contentUTF8() == null ? "" : e.contentUTF8();
        int status = e.status();

        if (status == 400 || cuerpo.contains("numero_cuenta_invalido")) {
            return ApiException.numeroCuentaInvalido();
        }
        if (status == 401 || cuerpo.contains("pin_invalido")) {
            return ApiException.pinAutorizacionInvalido("El PIN de autorizacion es incorrecto");
        }
        if (status == 403 || cuerpo.contains("acceso_denegado") || cuerpo.contains("usuario_inactivo")) {
            return ApiException.sinPermisos();
        }
        if (status == 404 || cuerpo.contains("cuenta_no_encontrada")) {
            return ApiException.numeroCuentaInvalido();
        }
        if (status == 423 || cuerpo.contains("pin_bloqueado") || cuerpo.contains("usuario_bloqueado")) {
            return ApiException.pinAutorizacionBloqueado(
                    "El PIN de autorizacion se encuentra bloqueado temporalmente por superar los 3 intentos fallidos");
        }
        if (status == 409) {
            if (cuerpo.contains("idempotencia_conflicto")) {
                return ApiException.conflictoIdempotencia();
            }
            return ApiException.cuentaBancariaInactiva(
                    "La cuenta " + ValidadorLuhn.enmascararCuenta(numeroCuenta) + " o el cliente no estan activos");
        }
        if (status == 422) {
            if (cuerpo.contains("pin_no_configurado")) {
                return ApiException.pinAutorizacionInvalido(
                        "El usuario no tiene un PIN de autorizacion de 4 digitos configurado");
            }
            return ApiException.saldoInsuficiente();
        }
        log.error(
                "Error de comunicacion con onboarding-clientes status={} cuenta={}",
                status,
                ValidadorLuhn.enmascararCuenta(numeroCuenta),
                e);
        return ApiException.servicioBancarioNoDisponible(e);
    }
}
