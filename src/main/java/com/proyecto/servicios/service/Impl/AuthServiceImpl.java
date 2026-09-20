package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.entity.auth.RefreshToken;
import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.RefreshRequest;
import com.proyecto.servicios.model.auth.RegisterRequest;
import com.proyecto.servicios.model.auth.TokenResponse;
import com.proyecto.servicios.repositorys.auth.RefreshTokenRepository;
import com.proyecto.servicios.repositorys.auth.RolRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTtlDays;
    private final String dummyPasswordHash;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           @Value("${security.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtlDays = refreshTtlDays;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash);
            throw ApiException.authInvalida();
        }
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw ApiException.authInvalida();
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ApiException.cuentaInactiva();
        }

        String access = jwtService.generarAccessToken(usuario);
        String refresh = guardarRefreshToken(usuario);
        log.info("Login exitoso email={}", usuario.getEmail());
        return TokenResponse.ok(access, refresh, jwtService.getAccessTtlSegundos(),
                usuario.getNombre(), roles(usuario));
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.getEmail();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw ApiException.emailYaRegistrado();
        }
        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol CLIENTE. Revisar la migracion R__seed_roles.sql"));

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(request.getNombre());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setRoles(Set.of(rolCliente));
        usuarioRepository.save(usuario);

        String access = jwtService.generarAccessToken(usuario);
        String refresh = guardarRefreshToken(usuario);
        log.info("Registro exitoso email={}", email);
        return TokenResponse.ok(access, refresh, jwtService.getAccessTtlSegundos(),
                usuario.getNombre(), roles(usuario));
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(jwtService.hash(request.getRefreshToken()))
                .orElseThrow(ApiException::refreshInvalido);

        if (Boolean.TRUE.equals(token.getRevocado()) || token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevocado(true);
            throw ApiException.refreshInvalido();
        }

        Usuario usuario = token.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            token.setRevocado(true);
            throw ApiException.cuentaInactiva();
        }

        token.setRevocado(true);
        refreshTokenRepository.save(token);

        String access = jwtService.generarAccessToken(usuario);
        String refresh = guardarRefreshToken(usuario);
        log.info("Refresh exitoso email={}", usuario.getEmail());
        return TokenResponse.ok(access, refresh, jwtService.getAccessTtlSegundos(),
                usuario.getNombre(), roles(usuario));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(jwtService.hash(refreshToken))
                .ifPresent(t -> t.setRevocado(true));
    }

    private String guardarRefreshToken(Usuario usuario) {
        String raw = jwtService.generarRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(jwtService.hash(raw));
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshTtlDays)));
        token.setRevocado(false);
        token.setCreatedAt(Instant.now());
        refreshTokenRepository.save(token);
        return raw;
    }

    private List<String> roles(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(r -> r.getNombre())
                .sorted()
                .toList();
    }
}