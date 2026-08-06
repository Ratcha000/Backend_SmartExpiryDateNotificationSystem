package com.app.expiry_system.usage.dto;

import com.app.expiry_system.usage.entity.UsageActionType;
import java.math.BigDecimal;
import java.time.Instant;

public class UsageHistoryResponse {

    private String id;
    private String ingredientId;
    private String ingredientName;
    private UsageActionType actionType;
    private BigDecimal quantityChanged;
    private String unit;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private String performedBy;
    private Instant performedAt;
    private String restaurantId;
    private String note;

    public UsageHistoryResponse(String id, String ingredientId, String ingredientName, UsageActionType actionType,
                                BigDecimal quantityChanged, String unit, BigDecimal quantityBefore,
                                BigDecimal quantityAfter, String performedBy, Instant performedAt,
                                String restaurantId, String note) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.actionType = actionType;
        this.quantityChanged = quantityChanged;
        this.unit = unit;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.restaurantId = restaurantId;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public UsageActionType getActionType() {
        return actionType;
    }

    public BigDecimal getQuantityChanged() {
        return quantityChanged;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getNote() {
        return note;
    }
}
