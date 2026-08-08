package com.app.expiry_system.suggestion.dto;

import java.util.List;

public class MenuSuggestionResponse {

    private String restaurantId;
    private List<String> sourceIngredients;
    private List<SuggestedMenuResponse> menus;

    public MenuSuggestionResponse(String restaurantId, List<String> sourceIngredients, List<SuggestedMenuResponse> menus) {
        this.restaurantId = restaurantId;
        this.sourceIngredients = sourceIngredients;
        this.menus = menus;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public List<String> getSourceIngredients() {
        return sourceIngredients;
    }

    public List<SuggestedMenuResponse> getMenus() {
        return menus;
    }
}
