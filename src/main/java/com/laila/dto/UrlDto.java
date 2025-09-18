package com.laila.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

@Schema(name = "UrlCreateRequest", description = "Request to create a short URL")
public class UrlDto {
    @Schema(description = "Original URL to shorten", example = "https://www.wikipedia.org", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String longUrl;

    @Schema(description = "Short url/ alias")
    private String shortUrl;

    @Schema(description = "Optional TTL in seconds (Redis expiry). Null/0 = never expire", example = "86400")
    private Long ttlSeconds;

    @Schema(description = "Expiration datetime of url")
    private Date expirationDate;

    @Schema(description = "Creation time of url")
    private Date creationDate;

    @Schema(description = "Optional user id who owns the link", example = "user-123")
    private String userId;

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
