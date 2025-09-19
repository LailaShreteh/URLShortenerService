package com.laila.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls",
        indexes = {
                @Index(name = "idx_short_urls_user", columnList = "user_id"),
                @Index(name = "idx_short_urls_expires", columnList = "expires_at"),
                @Index(name = "uq_short_urls_url_hash", columnList = "url_hash", unique = true) // optional
        })
public class Url {

    @Id
    @Column(name = "code", length = 10, nullable = false, updatable = false)
    private String code;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    @Column(name = "url_hash") // optional (only if you want idempotency)
    private String urlHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false;

    @Column(name = "status", nullable = false)
    private Short status = 1;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (isCustom == null) isCustom = false;
        if (status == null) status = 1;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public void setUrlHash(String urlHash) {
        this.urlHash = urlHash;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getCustom() {
        return isCustom;
    }

    public void setCustom(Boolean custom) {
        isCustom = custom;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }
}