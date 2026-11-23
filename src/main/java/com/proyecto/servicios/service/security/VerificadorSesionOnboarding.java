package com.proyecto.servicios.service.security;

import com.proyecto.servicios.client.OnboardingSesionClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprueba que la sesion de un token de {@code onboarding-clientes} siga vigente.
 *
 * <p>La administracion de sesiones es centralizada: onboarding es la unica fuente de verdad, de modo que
 * cerrar sesion en la web o iniciar sesion en el movil invalida el token tambien en este servicio. Las
 * respuestas positivas se recuerdan unos segundos por sesion para no consultar en cada peticion; esa es
 * la latencia maxima de una revocacion. Ante cualquier error se rechaza (fail closed).
 */
@Component
@Slf4j
public class VerificadorSesionOnboarding {

    private static final int MAX_ENTRADAS = 10_000;

    private final OnboardingSesionClient cliente;
    private final long cacheMs;
    private final Map<String, Long> vigentesHasta = new ConcurrentHashMap<>();

    public VerificadorSesionOnboarding(
            OnboardingSesionClient cliente,
            @Value("${onboarding.sesion.cache:PT15S}") Duration cache) {
        this.cliente = cliente;
        this.cacheMs = cache.toMillis();
    }

    public boolean esVigente(String sesionId, String token) {
        long ahora = System.currentTimeMillis();
        Long hasta = vigentesHasta.get(sesionId);
        if (hasta != null && hasta > ahora) {
            return true;
        }
        try {
            cliente.consultarSesion("Bearer " + token);
            if (vigentesHasta.size() > MAX_ENTRADAS) {
                vigentesHasta.entrySet().removeIf(e -> e.getValue() <= ahora);
            }
            vigentesHasta.put(sesionId, ahora + cacheMs);
            return true;
        } catch (FeignException.Unauthorized e) {
            vigentesHasta.remove(sesionId);
            return false;
        } catch (RuntimeException e) {
            log.warn("No fue posible verificar la sesion en onboarding-clientes: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /** Olvida una sesion (por ejemplo, al recibir un 401 de onboarding en otra llamada). */
    public void olvidar(String sesionId) {
        vigentesHasta.remove(sesionId);
    }
}
