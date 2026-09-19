package com.proyecto.servicios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "security.rate-limit.login.max=3",
        "security.rate-limit.login.window-seconds=60",
        "security.rate-limit.api.max=5",
        "security.rate-limit.api.window-seconds=60"
})
class RateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private TestRestTemplate rest;

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate();
        for (String key : redisTemplate.keys("rate:*")) {
            redisTemplate.delete(key);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void loginSeLimitaPorIP() {
        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map> response = login("rl@test.com", "Password123");
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
        ResponseEntity<Map> excedido = login("rl@test.com", "Password123");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, excedido.getStatusCode());
        assertEquals("RATE-001", excedido.getBody().get("code"));
    }

    @Test
    void apiSeLimitaPorUsuarioAutenticado() {
        TokenBody admin = loginOk("admin@test.com", "Admin-12345");
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> perfil = perfil(admin.accessToken());
            assertEquals(HttpStatus.OK, perfil.getStatusCode());
        }
        ResponseEntity<Map> excedido = perfil(admin.accessToken());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, excedido.getStatusCode());
        assertEquals("RATE-001", excedido.getBody().get("code"));
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
        return new TokenBody((String) body.get("accessToken"));
    }

    private ResponseEntity<Map> perfil(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url("/perfil"), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private record TokenBody(String accessToken) {
    }
}