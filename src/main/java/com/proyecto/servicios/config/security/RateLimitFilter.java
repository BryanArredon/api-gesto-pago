package com.proyecto.servicios.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.ErrorResponse;
import com.proyecto.servicios.service.security.ResilientRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String ENDPOINT_LOGIN = "login";
    private static final String ENDPOINT_API = "api";
    private static final Set<String> PATHS_SIN_LIMITE = Set.of(
            "/auth/login", "/auth/refresh",
            "/v3/api-docs", "/swagger-ui", "/actuator/health", "/error");

    private final ResilientRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${security.rate-limit.login.max}")
    private long loginMax;

    @Value("${security.rate-limit.login.window-seconds}")
    private long loginWindowSeconds;

    @Value("${security.rate-limit.api.max}")
    private long apiMax;

    @Value("${security.rate-limit.api.window-seconds}")
    private long apiWindowSeconds;

    public RateLimitFilter(ResilientRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = pathDe(request);

        String subject;
        String endpoint;
        long max;
        Duration window;

        if ("/auth/login".equals(path) && HttpMethod.POST.matches(request.getMethod())) {
            subject = ipCliente(request);
            endpoint = ENDPOINT_LOGIN;
            max = loginMax;
            window = Duration.ofSeconds(loginWindowSeconds);
        } else if (excluido(path)) {
            filterChain.doFilter(request, response);
            return;
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                filterChain.doFilter(request, response);
                return;
            }
            subject = authentication.getName();
            endpoint = ENDPOINT_API;
            max = apiMax;
            window = Duration.ofSeconds(apiWindowSeconds);
        }

        if (!rateLimiter.tryConsume(subject, endpoint, max, window)) {
            log.warn("Rate limit excedido endpoint={} subject={} ruta={}",
                    endpoint, subject, path);
            ApiException api = ApiException.limiteAlcanzado();
            response.setStatus(api.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse(api.getCode(), api.getMessage(), MDC.get(TraceIdFilter.TRACE_ID)));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean excluido(String path) {
        return PATHS_SIN_LIMITE.stream().anyMatch(prefix ->
                prefix.equals(path) || path.startsWith(prefix + "/"));
    }

    private String pathDe(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
    }

    private String ipCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            List<String> partes = java.util.Arrays.asList(forwarded.split(","));
            return partes.get(0).trim();
        }
        String ip = request.getRemoteAddr();
        return ip == null ? "desconocido" : ip;
    }
}