package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.auth.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROL_CLAIM = "rol";
    private static final String CORREO_CLAIM = "correo";
    private static final String EMISOR_ONBOARDING_DEFECTO = "onboarding-clientes";

    private final SecretKey key;
    private final String issuer;
    private final Set<String> emisoresConfiables;
    private final long accessTtlMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.access-ttl-minutes}") long accessTtlMinutes) {
        this(secret, issuer, accessTtlMinutes, issuer + "," + EMISOR_ONBOARDING_DEFECTO);
    }

    public JwtService(String secret,
                      String issuer,
                      long accessTtlMinutes,
                      String trustedIssuersCsv) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes para HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
        Set<String> emisores = new LinkedHashSet<>();
        if (issuer != null && !issuer.isBlank()) {
            emisores.add(issuer.trim());
        }
        emisores.add(EMISOR_ONBOARDING_DEFECTO);
        if (trustedIssuersCsv != null && !trustedIssuersCsv.isBlank()) {
            Arrays.stream(trustedIssuersCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(emisores::add);
        }
        this.emisoresConfiables = Set.copyOf(emisores);
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
                .claim("correo", usuario.getEmail())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtlMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    public String generarTokenHomologado(String email, String rol) {
        long now = System.currentTimeMillis();
        String autoridad = rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;
        String nombreRol = rol.startsWith("ROLE_") ? rol.substring(5) : rol;
        return Jwts.builder()
                .issuer(issuer)
                .subject(email)
                .claim("correo", email)
                .claim("rol", nombreRol)
                .claim(ROLES_CLAIM, List.of(autoridad))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 120_000L))
                .signWith(key)
                .compact();
    }

    public Claims validarAccessToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String tokenIssuer = claims.getIssuer();
        if (tokenIssuer == null || !emisoresConfiables.contains(tokenIssuer)) {
            throw new JwtException("Emisor JWT no confiable: " + tokenIssuer);
        }
        return claims;
    }

    /**
     * Extrae el correo electronico canonico del usuario desde un JWT emitido por
     * {@code onboarding-clientes} (claim {@code correo}, subject UUID) o por {@code api-gesto-pago}
     * (subject email).
     */
    public String extraerEmail(Claims claims) {
        String correo = claims.get(CORREO_CLAIM, String.class);
        if (correo != null && !correo.isBlank()) {
            return correo.trim().toLowerCase(Locale.ROOT);
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("El token JWT no contiene correo ni subject");
        }
        return subject.trim().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(Claims claims) {
        List<String> roles = claims.get(ROLES_CLAIM, List.class);
        if (roles != null && !roles.isEmpty()) {
            return roles.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .collect(Collectors.toList());
        }
        String rolUnico = claims.get(ROL_CLAIM, String.class);
        if (rolUnico != null && !rolUnico.isBlank()) {
            String limpio = rolUnico.trim();
            return List.of(limpio.startsWith("ROLE_") ? limpio : "ROLE_" + limpio);
        }
        return List.of("ROLE_CLIENTE");
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