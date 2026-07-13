package com.app.expiry_system.restaurant.dto;

import java.time.Instant;

public class RestaurantResponse {

    private String id;
    private String name;
    private String managerId;
    private String inviteCode;
    private Instant createdAt;
    private Instant updatedAt;

    public RestaurantResponse(String id, String name, String managerId, String inviteCode, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.managerId = managerId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
