package com.proyecto.servicios.service.security;

import java.time.Duration;

public interface RateLimiter {

    boolean tryConsume(String subject, String endpoint, long max, Duration window);
}