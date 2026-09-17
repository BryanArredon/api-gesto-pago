package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.entity.auth.RefreshToken;
import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.RefreshRequest;
import com.proyecto.servicios.model.auth.TokenResponse;
import com.proyecto.servicios.repositorys.auth.RefreshTokenRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "clave-super-segura-para-tests-jwt-2026-que-excede-32-bytes";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(SECRET, "test-issuer", 15);
    private AuthServiceImpl service;

    private Usuario admin;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(usuarioRepository, refreshTokenRepository,
                passwordEncoder, jwtService, 7);
        admin = new Usuario();
        admin.setId(1L);
        admin.setEmail("admin@test.com");
        admin.setNombre("Admin Test");
        admin.setPasswordHash(passwordEncoder.encode("S3creto!"));
        admin.setActivo(true);
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMIN");
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        admin.setRoles(Set.of(rolAdmin, rolCliente));
    }

    @Test
    void loginExitosoDevuelveTokensYRoles() {
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LoginRequest request = new LoginRequest();
        request.setEmail(" admin@test.com ");
        request.setPassword("S3creto!");

        TokenResponse response = service.login(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertTrue(response.getRoles().contains("ADMIN"));
        assertTrue(response.getRoles().contains("CLIENTE"));
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void loginConPasswordEquivocadaDevuelveCredencialesInvalidas() {
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("incorrecta");

        ApiException error = assertThrows(ApiException.class, () -> service.login(request));
        assertEquals("AUTH-001", error.getCode());
    }

    @Test
    void loginDeUsuarioInexistenteNoRevelaExistencia() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("nadie@test.com");
        request.setPassword("S3creto!");

        ApiException error = assertThrows(ApiException.class, () -> service.login(request));
        assertEquals("AUTH-001", error.getCode());
    }

    @Test
    void loginDeCuentaInactivaEsRechazado() {
        admin.setActivo(false);
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("S3creto!");

        ApiException error = assertThrows(ApiException.class, () -> service.login(request));
        assertEquals("AUTH-002", error.getCode());
    }

    @Test
    void refreshRotaElTokenAnterior() {
        RefreshToken token = new RefreshToken();
        token.setUsuario(admin);
        token.setTokenHash(jwtService.hash("refresh-viejo"));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevocado(false);
        when(refreshTokenRepository.findByTokenHash(jwtService.hash("refresh-viejo")))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-viejo");

        TokenResponse response = service.refresh(request);

        assertTrue(token.getRevocado());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().equals("refresh-viejo"));
        verify(refreshTokenRepository, atMost(3)).save(any(RefreshToken.class));
    }

    @Test
    void refreshExpiradoEsRechazadoYRevocado() {
        RefreshToken token = new RefreshToken();
        token.setUsuario(admin);
        token.setTokenHash(jwtService.hash("refresh-expirado"));
        token.setExpiresAt(Instant.now().minusSeconds(10));
        token.setRevocado(false);
        when(refreshTokenRepository.findByTokenHash(jwtService.hash("refresh-expirado")))
                .thenReturn(Optional.of(token));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-expirado");

        ApiException error = assertThrows(ApiException.class, () -> service.refresh(request));
        assertEquals("AUTH-003", error.getCode());
        assertTrue(token.getRevocado());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshInexistenteEsRechazado() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("desconocido");

        ApiException error = assertThrows(ApiException.class, () -> service.refresh(request));
        assertEquals("AUTH-003", error.getCode());
    }

    @Test
    void logoutRevocaElTokenSiExiste() {
        RefreshToken token = new RefreshToken();
        token.setUsuario(admin);
        token.setTokenHash(jwtService.hash("refresh-a-revocar"));
        token.setRevocado(false);
        when(refreshTokenRepository.findByTokenHash(jwtService.hash("refresh-a-revocar")))
                .thenReturn(Optional.of(token));

        service.logout("refresh-a-revocar");

        assertTrue(token.getRevocado());
    }

    @Test
    void logoutNullEsInofensivo() {
        service.logout(null);
        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }
}