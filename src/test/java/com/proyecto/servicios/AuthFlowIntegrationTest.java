package com.proyecto.servicios;

import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.repositorys.auth.RolRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class AuthFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestRestTemplate rest;

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void loginConCredencialesErroneasDevuelve401() {
        ResponseEntity<Map> response = login("admin@test.com", "Password-inexistente");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTH-001", response.getBody().get("code"));
    }

    @Test
    void endpointProtegidoRequiereToken() {
        ResponseEntity<Map> response = rest.getForEntity(url("/perfil"), Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTH-005", response.getBody().get("code"));
    }

    @Test
    void flujoCompletoLoginYAccesoAutenticado() {
        TokenBody admin = loginOk("admin@test.com", "Admin-12345");
        assertNotNull(admin.accessToken());
        assertTrue(admin.roles().contains("ADMIN"));

        ResponseEntity<Map> perfil = llamadaAutenticada("/perfil", admin.accessToken());
        assertEquals(HttpStatus.OK, perfil.getStatusCode());
        assertEquals("admin@test.com", perfil.getBody().get("email"));

        ResponseEntity<Map> adminPing = llamadaAutenticada("/admin/ping", admin.accessToken());
        assertEquals(HttpStatus.OK, adminPing.getStatusCode());
    }

    @Test
    void clienteNoPuedeAccederAdmin() {
        Usuario cliente = crearCliente("cliente@test.com", "Passw0rd!");
        TokenBody token = loginOk(cliente.getEmail(), "Passw0rd!");

        ResponseEntity<Map> adminPing = llamadaAutenticada("/admin/ping", token.accessToken());
        assertEquals(HttpStatus.FORBIDDEN, adminPing.getStatusCode());
        assertEquals("PERM-001", adminPing.getBody().get("code"));
    }

    @Test
    void refreshRotaElTokenYElAnteriorDejaDeServir() {
        TokenBody admin = loginOk("admin@test.com", "Admin-12345");

        ResponseEntity<Map> refresh = refrescar(admin.refreshToken());
        assertEquals(HttpStatus.OK, refresh.getStatusCode());
        String nuevoRefresh = (String) refresh.getBody().get("refreshToken");
        assertFalse(nuevoRefresh.equals(admin.refreshToken()));

        ResponseEntity<Map> reuso = refrescar(admin.refreshToken());
        assertEquals(HttpStatus.UNAUTHORIZED, reuso.getStatusCode());
        assertEquals("AUTH-003", reuso.getBody().get("code"));
    }

    @Test
    void logoutRevocaElRefreshToken() {
        TokenBody admin = loginOk("admin@test.com", "Admin-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Refresh " + admin.refreshToken());
        ResponseEntity<Void> logout = rest.exchange(url("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());

        ResponseEntity<Map> reuso = refrescar(admin.refreshToken());
        assertEquals(HttpStatus.UNAUTHORIZED, reuso.getStatusCode());
        assertEquals("AUTH-003", reuso.getBody().get("code"));
    }

    @Test
    void tokenConFirmaInvalidaEsRechazado() {
        ResponseEntity<Map> response = llamadaAutenticada("/perfil", "token.mal.formado");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTH-005", response.getBody().get("code"));
    }

    private Usuario crearCliente(String email, String password) {
        return usuarioRepository.findByEmail(email).orElseGet(() -> {
            Rol cliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setPasswordHash(passwordEncoder.encode(password));
            usuario.setNombre("Cliente Test");
            usuario.setActivo(true);
            usuario.setRoles(Set.of(cliente));
            return usuarioRepository.save(usuario);
        });
    }

    private ResponseEntity<Map> login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> body = new HttpEntity<>(
                Map.of("email", email, "password", password), headers);
        return rest.exchange(url("/auth/login"), HttpMethod.POST, body, Map.class);
    }

    private TokenBody loginOk(String email, String password) {
        ResponseEntity<Map> response = login(email, password);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        return new TokenBody((String) body.get("accessToken"),
                (String) body.get("refreshToken"), (java.util.List<String>) body.get("roles"));
    }

    private ResponseEntity<Map> refrescar(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> body = new HttpEntity<>(
                Map.of("refreshToken", refreshToken), headers);
        return rest.exchange(url("/auth/refresh"), HttpMethod.POST, body, Map.class);
    }

    private ResponseEntity<Map> llamadaAutenticada(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
    }

    private record TokenBody(String accessToken, String refreshToken, java.util.List<String> roles) {
    }
}