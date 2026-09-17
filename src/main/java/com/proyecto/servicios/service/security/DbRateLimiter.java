package com.proyecto.servicios.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@Slf4j
public class DbRateLimiter implements RateLimiter {

    private final String upsertSql;
    private final JdbcTemplate jdbcTemplate;

    public DbRateLimiter(JdbcTemplate jdbcTemplate,
                         @Value("${spring.flyway.schemas}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = "INSERT INTO " + schema + ".rate_limit_hits "
                + "(subject, endpoint, window_start, count) "
                + "VALUES (?, ?, ?, 1) "
                + "ON CONFLICT (subject, endpoint, window_start) "
                + "DO UPDATE SET count = " + schema + ".rate_limit_hits.count + 1 "
                + "RETURNING count";
    }

    @Override
    public boolean tryConsume(String subject, String endpoint, long max, Duration window) {
        long windowSeconds = Math.max(1, window.getSeconds());
        long epochSeconds = Instant.now().getEpochSecond();
        OffsetDateTime windowStart = Instant.ofEpochSecond((epochSeconds / windowSeconds) * windowSeconds)
                .atOffset(ZoneOffset.UTC);
        Integer count = jdbcTemplate.queryForObject(
                upsertSql, Integer.class, subject, endpoint, windowStart);
        boolean permitido = count != null && count <= max;
        if (!permitido) {
            log.warn("Rate limit excedido (BD) endpoint={} subject={} count={} max={}",
                    endpoint, subject, count, max);
        }
        return permitido;
    }
}