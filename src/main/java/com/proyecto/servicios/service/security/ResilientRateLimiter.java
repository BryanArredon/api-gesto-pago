package com.proyecto.servicios.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class ResilientRateLimiter implements RateLimiter {

    private final RedisRateLimiter redisRateLimiter;
    private final DbRateLimiter dbRateLimiter;

    public ResilientRateLimiter(RedisRateLimiter redisRateLimiter, DbRateLimiter dbRateLimiter) {
        this.redisRateLimiter = redisRateLimiter;
        this.dbRateLimiter = dbRateLimiter;
    }

    @Override
    public boolean tryConsume(String subject, String endpoint, long max, Duration window) {
        try {
            return redisRateLimiter.tryConsume(subject, endpoint, max, window);
        } catch (Exception redisError) {
            log.warn("Redis indisponible para rate limit, usando fallback BD: {}", redisError.getMessage());
        }
        try {
            return dbRateLimiter.tryConsume(subject, endpoint, max, window);
        } catch (Exception dbError) {
            log.error("Rate limit sin fallback disponible (Redis y BD), permitiendo peticion: {}",
                    dbError.getMessage());
            return true;
        }
    }
}