package com.app.expiry_system.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 100, message = "Restaurant name must be under 100 characters")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
