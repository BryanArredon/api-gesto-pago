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

        assertThrows(ExpiredJwtException.class,
                () -> service.validarAccessToken(service.generarAccessToken(usuario())));
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