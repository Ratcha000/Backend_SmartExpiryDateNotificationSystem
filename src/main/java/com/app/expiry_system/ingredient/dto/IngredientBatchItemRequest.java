package com.app.expiry_system.ingredient.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class IngredientBatchItemRequest {

    @NotBlank
    private String partName;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal initialQuantity;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal quantity;

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(BigDecimal initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
