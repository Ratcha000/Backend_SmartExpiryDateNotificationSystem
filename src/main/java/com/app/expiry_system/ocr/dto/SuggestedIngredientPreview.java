package com.app.expiry_system.ocr.dto;

import java.math.BigDecimal;

public class SuggestedIngredientPreview {

    private String name;
    private String category;
    private BigDecimal quantity;
    private String unit;

    public SuggestedIngredientPreview(String name, String category, BigDecimal quantity, String unit) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getName() {
        return name;
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
}
