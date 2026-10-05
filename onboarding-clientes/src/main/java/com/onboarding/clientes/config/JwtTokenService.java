package com.onboarding.clientes.config;

import com.onboarding.clientes.cliente.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio para la generacion, firma y validacion de tokens JWT propios.
 */
@Service
public class JwtTokenService {

    private final Key signingKey;
    private final String emisor;
    private final long duracionAccesoSegundos;
    private final long duracionRefreshSegundos;

    public JwtTokenService(
            @Value("${onboarding.token.secreto:onboarding-clientes-secret-key-must-be-at-least-256-bits-long-for-hmac-sha-256}") String secreto,
            PropiedadesOnboarding propiedades) {
        byte[] keyBytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Relleno seguro si el string fuera mas corto
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            this.signingKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        this.emisor = propiedades.token().emisor();
        this.duracionAccesoSegundos = propiedades.token().duracionAcceso().toSeconds();
        this.duracionRefreshSegundos = propiedades.token().duracionRefresh().toSeconds();
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(duracionAccesoSegundos);

        return Jwts.builder()
                .setSubject(usuario.id().toString())
                .setIssuer(emisor)
                .setAudience("onboarding-api")
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(expira))
                .claim("clienteId", usuario.clienteId().toString())
                .claim("correo", usuario.correo().valor())
                .claim("roles", List.of("ROLE_CLIENTE"))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generarRefreshToken(Usuario usuario, UUID familiaId) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(duracionRefreshSegundos);

        return Jwts.builder()
                .setSubject(usuario.id().toString())
                .setIssuer(emisor)
                .setAudience("onboarding-api-refresh")
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(expira))
                .claim("tipo", "refresh")
                .claim("familiaId", familiaId.toString())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long getDuracionAccesoSegundos() {
        return duracionAccesoSegundos;
    }
}
