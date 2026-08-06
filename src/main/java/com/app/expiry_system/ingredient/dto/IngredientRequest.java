package com.app.expiry_system.ingredient.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class IngredientRequest {

    @NotBlank
    private String restaurantId;

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @NotNull
    @DecimalMin(value = "0.001")
    private BigDecimal initialQuantity;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal quantity;

    @NotBlank
    private String unit;

    private String categoryUnitHint;

    @NotNull
    private LocalDate expiryDate;

    @NotNull
    @Min(0)
    private Integer notifyDaysBefore;

    private String scannedBy;

    private Instant scannedAt;

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategoryUnitHint() {
        return categoryUnitHint;
    }

    public void setCategoryUnitHint(String categoryUnitHint) {
        this.categoryUnitHint = categoryUnitHint;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getNotifyDaysBefore() {
        return notifyDaysBefore;
    }

    public void setNotifyDaysBefore(Integer notifyDaysBefore) {
        this.notifyDaysBefore = notifyDaysBefore;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }
}
