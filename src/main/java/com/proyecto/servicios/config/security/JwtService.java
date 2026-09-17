package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.auth.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {

    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final String issuer;
    private final long accessTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.access-ttl-minutes}") long accessTtlMinutes) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes para HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public String generarAccessToken(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(r -> "ROLE_" + r.getNombre())
                .sorted()
                .toList();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .subject(usuario.getEmail())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtlMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    public Claims validarAccessToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generarRefreshToken() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public long getAccessTtlSegundos() {
        return accessTtlMinutes * 60L;
    }
}