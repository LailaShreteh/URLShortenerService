package com.laila.dto;


import io.swagger.v3.oas.annotations.media.Schema;

public class UserDto {

    @Schema(description = "Unique User ID")
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
