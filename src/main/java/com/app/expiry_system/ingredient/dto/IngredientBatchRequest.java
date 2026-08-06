package com.app.expiry_system.ingredient.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class IngredientBatchRequest {

    @NotBlank
    private String restaurantId;

    @NotBlank
    private String lotName;

    @NotBlank
    private String category;

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

    @NotEmpty
    private List<@jakarta.validation.Valid IngredientBatchItemRequest> items;

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public List<IngredientBatchItemRequest> getItems() {
        return items;
    }

    public void setItems(List<IngredientBatchItemRequest> items) {
        this.items = items;
    }
}
