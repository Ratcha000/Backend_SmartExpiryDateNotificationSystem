package com.app.expiry_system.suggestion.dto;

import java.util.List;

public class IngredientMenuSuggestionResponse {

    private String restaurantId;
    private String ingredientName;
    private List<SuggestedMenuResponse> menus;

    public IngredientMenuSuggestionResponse(String restaurantId, String ingredientName, List<SuggestedMenuResponse> menus) {
        this.restaurantId = restaurantId;
        this.ingredientName = ingredientName;
        this.menus = menus;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public List<SuggestedMenuResponse> getMenus() {
        return menus;
    }
}
