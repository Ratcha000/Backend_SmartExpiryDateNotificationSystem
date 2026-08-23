package com.app.expiry_system.purchase.dto;

import jakarta.validation.constraints.NotBlank;

public class PurchaseRecommendationGenerateRequest {

    @NotBlank
    private String restaurantId;

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

}
