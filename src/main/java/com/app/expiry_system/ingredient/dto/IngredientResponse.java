package com.app.expiry_system.ingredient.dto;

import com.app.expiry_system.ingredient.entity.IngredientStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class IngredientResponse {

    private String id;
    private String restaurantId;
    private String name;
    private String lotId;
    private String lotName;
    private String category;
    private BigDecimal initialQuantity;
    private BigDecimal quantity;
    private String unit;
    private String categoryUnitHint;
    private LocalDate expiryDate;
    private Integer notifyDaysBefore;
    private IngredientStatus status;
    private long daysLeft;
    private boolean expiring;
    private boolean expired;
    private String scannedBy;
    private Instant scannedAt;
    private Instant lastUsedAt;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public IngredientResponse(String id, String restaurantId, String name, String lotId, String lotName, String category, BigDecimal initialQuantity,
                              BigDecimal quantity, String unit, String categoryUnitHint, LocalDate expiryDate,
                              Integer notifyDaysBefore, IngredientStatus status, long daysLeft, boolean expiring,
                              boolean expired, String scannedBy, Instant scannedAt, Instant lastUsedAt, String updatedBy,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.lotId = lotId;
        this.lotName = lotName;
        this.category = category;
        this.initialQuantity = initialQuantity;
        this.quantity = quantity;
        this.unit = unit;
        this.categoryUnitHint = categoryUnitHint;
        this.expiryDate = expiryDate;
        this.notifyDaysBefore = notifyDaysBefore;
        this.status = status;
        this.daysLeft = daysLeft;
        this.expiring = expiring;
        this.expired = expired;
        this.scannedBy = scannedBy;
        this.scannedAt = scannedAt;
        this.lastUsedAt = lastUsedAt;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public String getLotId() { return lotId; }
    public String getLotName() { return lotName; }
    public String getCategory() { return category; }
    public BigDecimal getInitialQuantity() { return initialQuantity; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getCategoryUnitHint() { return categoryUnitHint; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Integer getNotifyDaysBefore() { return notifyDaysBefore; }
    public IngredientStatus getStatus() { return status; }
    public long getDaysLeft() { return daysLeft; }
    public boolean isExpiring() { return expiring; }
    public boolean isExpired() { return expired; }
    public String getScannedBy() { return scannedBy; }
    public Instant getScannedAt() { return scannedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
