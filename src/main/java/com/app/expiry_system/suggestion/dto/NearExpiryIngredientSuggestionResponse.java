package com.app.expiry_system.suggestion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class NearExpiryIngredientSuggestionResponse {

    private String ingredientId;
    private String ingredientName;
    private String category;
    private BigDecimal quantity;
    private String unit;
    private LocalDate expiryDate;
    private long daysLeft;
    private List<SuggestedMenuResponse> menus;

    public NearExpiryIngredientSuggestionResponse(String ingredientId, String ingredientName, String category,
                                                  BigDecimal quantity, String unit, LocalDate expiryDate,
                                                  long daysLeft, List<SuggestedMenuResponse> menus) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
        this.daysLeft = daysLeft;
        this.menus = menus;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public long getDaysLeft() {
        return daysLeft;
    }

    public List<SuggestedMenuResponse> getMenus() {
        return menus;
    }
}
