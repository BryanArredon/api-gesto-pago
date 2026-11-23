package com.proyecto.servicios.config.security;

import com.proyecto.servicios.service.security.VerificadorSesionOnboarding;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final VerificadorSesionOnboarding verificadorSesion;

    public JwtAuthenticationFilter(JwtService jwtService, VerificadorSesionOnboarding verificadorSesion) {
        this.jwtService = jwtService;
        this.verificadorSesion = verificadorSesion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length());
            try {
                Claims claims = jwtService.validarAccessToken(token);
                // Sesion centralizada: un token de una sesion cerrada o reemplazada ya no autentica aqui.
                if (jwtService.esDeOnboarding(claims)
                        && !verificadorSesion.esVigente(claims.get(JwtService.CLAIM_SESION, String.class), token)) {
                    throw new JwtException("La sesion del token ya no esta vigente");
                }
                String emailPrincipal = jwtService.extraerEmail(claims);
                List<SimpleGrantedAuthority> authorities = jwtService.extraerRoles(claims).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(emailPrincipal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                log.warn("Token JWT invalido: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}