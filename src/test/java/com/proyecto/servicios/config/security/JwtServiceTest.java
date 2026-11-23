package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "clave-super-segura-para-tests-jwt-2026-que-excede-32-bytes";

    @Test
    void generarYValidarAccessToken() {
        JwtService service = new JwtService(SECRET, "test-issuer", 15);
        Usuario usuario = usuario();

        String token = service.generarAccessToken(usuario);
        Claims claims = service.validarAccessToken(token);

        assertEquals("admin@test.com", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertTrue(claims.get("roles", List.class).contains("ROLE_ADMIN"));
        assertTrue(claims.get("roles", List.class).contains("ROLE_CLIENTE"));
    }

    @Test
    void tokenGeneradoConOtraFirmaEsRechazado() {
        JwtService emisor = new JwtService(SECRET, "test-issuer", 15);
        JwtService receptor = new JwtService(
                "otra-clave-distinta-para-rechazar-firmas-en-tests-2026-long", "test-issuer", 15);

        String token = emisor.generarAccessToken(usuario());
        assertThrows(JwtException.class, () -> receptor.validarAccessToken(token));
    }

    @Test
    void tokenExpiradoEsRechazado() {
        JwtService service = new JwtService(SECRET, "test-issuer", -1);
        String token = service.generarAccessToken(usuario());

        assertThrows(ExpiredJwtException.class,
                () -> service.validarAccessToken(token));
    }

    @Test
    void secretCortoLanzaErrorAlConstruir() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", "issuer", 15));
    }

    @Test
    void hashEsDeterministicoYPreferibleParaBuscar() {
        JwtService service = new JwtService(SECRET, "test-issuer", 15);
        assertEquals(service.hash("abc"), service.hash("abc"));
        assertTrue(service.hash("abc").length() >= 43);
    }

    @Test
    void aceptaTokenEmitidoPorOnboardingClientesYExtraeCorreoYRol() {
        JwtService receptorGestoPago = new JwtService(SECRET, "prueba-servicios", 15);

        String tokenOnboarding = tokenOnboarding("onboarding-api", java.util.UUID.randomUUID().toString());

        Claims claims = receptorGestoPago.validarAccessToken(tokenOnboarding);
        assertNotNull(claims);
        assertEquals("cliente.banco@utng.edu.mx", receptorGestoPago.extraerEmail(claims));
        assertEquals(List.of("ROLE_CLIENTE"), receptorGestoPago.extraerRoles(claims));
        assertTrue(receptorGestoPago.esDeOnboarding(claims));
    }

    @Test
    void rechazaTokenDeRefrescoODesafioMfaDeOnboarding() {
        JwtService receptor = new JwtService(SECRET, "prueba-servicios", 15);
        String sid = java.util.UUID.randomUUID().toString();

        // Mismo emisor y misma firma, pero no son tokens de acceso: no deben autenticar aqui.
        assertThrows(JwtException.class, () -> receptor.validarAccessToken(tokenOnboarding("onboarding-api-refresh", sid)));
        assertThrows(JwtException.class, () -> receptor.validarAccessToken(tokenOnboarding("onboarding-mfa", sid)));
    }

    @Test
    void rechazaTokenDeOnboardingSinSesion() {
        JwtService receptor = new JwtService(SECRET, "prueba-servicios", 15);
        assertThrows(JwtException.class, () -> receptor.validarAccessToken(tokenOnboarding("onboarding-api", null)));
    }

    private static String tokenOnboarding(String audiencia, String sid) {
        var builder = io.jsonwebtoken.Jwts.builder()
                .issuer("onboarding-clientes")
                .audience().add(audiencia).and()
                .subject(java.util.UUID.randomUUID().toString())
                .claim("clienteId", java.util.UUID.randomUUID().toString())
                .claim("correo", "Cliente.Banco@UTNG.edu.mx")
                .claim("rol", "CLIENTE")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000L));
        if (sid != null) {
            builder.claim("sid", sid);
        }
        return builder.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }

    private Usuario usuario() {
        Rol admin = new Rol();
        admin.setNombre("ADMIN");
        Rol cliente = new Rol();
        cliente.setNombre("CLIENTE");
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("admin@test.com");
        usuario.setNombre("Admin Test");
        usuario.setActivo(true);
        usuario.setRoles(Set.of(admin, cliente));
        return usuario;
    }
}