package com.laila.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.beans.Transient;
import java.time.Instant;
@RedisHash("urls")
public class Url {

    @Id
    private Long id;

    private String longUrl;

    @Indexed
    private String userId;

    private Instant createdAt = Instant.now();

    // Redis-enforced expiry (seconds). Null/0 => never expires
    @TimeToLive
    private Long ttlSeconds;

    @Transient
    public Instant getExpiresAt() {
        return (ttlSeconds == null || ttlSeconds <= 0) ? null : createdAt.plusSeconds(ttlSeconds);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Instant getCreatedDate() {
        return createdAt;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdAt = createdDate;
    }
}