package com.laila.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserDto", description = "User data returned to clients")
public class UserDto {

    @Schema(description = "User id", example = "42")
    private Long id;

    @Schema(description = "Display name", example = "Laila Shreteh")
    private String name;

    @Schema(description = "Email (case-insensitive)", example = "laila@example.com")
    private String email;

    @Schema(description = "Creation timestamp", example = "2025-01-10T11:22:33Z")
    private Instant createdAt;

    @Schema(description = "Status: 1=active, 2=disabled", example = "1")
    private Short status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }
}
