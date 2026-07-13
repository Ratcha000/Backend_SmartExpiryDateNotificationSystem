package com.app.expiry_system.auth.dto;

import com.app.expiry_system.auth.entity.UserRole;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

    @Size(max = 120, message = "Display name must be under 120 characters")
    private String displayName;

    private UserRole role;

    private String restaurantId;

    // Getters and Setters
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
