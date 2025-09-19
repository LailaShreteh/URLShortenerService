package com.laila.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;


@Component
public class UrlCache {

    private final StringRedisTemplate redis;
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public UrlCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get(String code) {
        return redis.opsForValue().get(key(code));
    }

    public void set(String code, String url, Instant expiresAt) {
        Duration ttl = DEFAULT_TTL;
        if (expiresAt != null) {
            long secs = Duration.between(Instant.now(), expiresAt).getSeconds();
            if (secs <= 0) { // already expired → ensure no cache
                redis.delete(key(code));
                return;
            }
            ttl = Duration.ofSeconds(Math.min(secs, DEFAULT_TTL.getSeconds()));
        }
        redis.opsForValue().set(key(code), url, ttl);
    }

    public void delete(String code) {
        redis.delete(key(code));
    }

    private String key(String code) {
        return "code:" + code;
    }
}