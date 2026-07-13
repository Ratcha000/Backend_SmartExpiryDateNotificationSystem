package com.app.expiry_system.auth.dto;

import com.app.expiry_system.auth.entity.UserRole;

public class UserResponse {

    private String id;
    private String email;
    private String displayName;
    private UserRole role;
    private String restaurantId;

    public UserResponse() {
    }

    public UserResponse(String id, String email, String displayName, UserRole role, String restaurantId) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.restaurantId = restaurantId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }
}