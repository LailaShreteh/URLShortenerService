package com.laila.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UrlDto", description = "Request to shorten a URL (alias/expiration optional)")
public class UrlDto {

    @NotBlank
    @Size(max = 2048, message = "URL is too long")
    @Pattern(regexp = "https?://.+", message = "URL must start with http:// or https://")
    @Schema(description = "Absolute target URL", example = "https://www.example.com/some/long/url")
    private String longUrl;

    // Optional custom alias; server enforces uniqueness in DB
    @Size(min = 3, max = 32, message = "Alias must be 3–32 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Alias may contain letters, numbers, _ and - only")
    @Schema(description = "Optional custom short code (must be unique)", example = "docs123")
    private String alias;

    @Schema(description = "Optional expiration instant", example = "2026-12-31T23:59:59Z")
    private Instant expirationDate;

    @Schema(description = "Optional owning user id")
    private Long userId;

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
