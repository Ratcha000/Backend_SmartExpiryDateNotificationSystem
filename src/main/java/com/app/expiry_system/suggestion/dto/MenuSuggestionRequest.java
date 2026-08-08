package com.app.expiry_system.suggestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MenuSuggestionRequest {

    @NotBlank
    private String restaurantId;

    @NotEmpty
    private List<@NotBlank String> ingredientNames;

    @Min(1)
    @Max(10)
    private Integer maxMenus = 5;

    private String language = "th";

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public List<String> getIngredientNames() {
        return ingredientNames;
    }

    public void setIngredientNames(List<String> ingredientNames) {
        this.ingredientNames = ingredientNames;
    }

    public Integer getMaxMenus() {
        return maxMenus;
    }

    public void setMaxMenus(Integer maxMenus) {
        this.maxMenus = maxMenus;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
