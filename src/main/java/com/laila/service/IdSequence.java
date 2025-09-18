package com.laila.service;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

/**
 * Generates unique numeric IDs using Redis INCR.
 */
@Component
public class IdSequence {
    private final RedisAtomicLong seq;

    public IdSequence(RedisConnectionFactory factory) {
        // Redis key that holds the counter value
        this.seq = new RedisAtomicLong("url:id:seq", factory);
    }

    public long next() {
        return seq.incrementAndGet(); // 1, 2, 3, ...
    }
}