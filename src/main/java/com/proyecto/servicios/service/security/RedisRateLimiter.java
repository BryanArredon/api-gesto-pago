package com.proyecto.servicios.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate:";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(String subject, String endpoint, long max, Duration window) {
        String windowKey = windowTruncada(window);
        String key = KEY_PREFIX + endpoint + ":" + subject + ":" + windowKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        boolean permitido = count != null && count <= max;
        if (!permitido) {
            log.warn("Rate limit excedido endpoint={} subject={} count={} max={}", endpoint, subject, count, max);
        }
        return permitido;
    }

    private String windowTruncada(Duration window) {
        long epoch = System.currentTimeMillis() / window.toMillis();
        return String.valueOf(epoch);
    }
}