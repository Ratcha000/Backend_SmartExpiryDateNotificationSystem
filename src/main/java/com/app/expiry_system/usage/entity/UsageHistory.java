package com.app.expiry_system.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_history")
public class UsageHistory {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String ingredientId;

    @Column(nullable = false, length = 120)
    private String ingredientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageActionType actionType;

    @Column(precision = 19, scale = 3)
    private BigDecimal quantityChanged;

    @Column(length = 30)
    private String unit;

    @Column(precision = 19, scale = 3)
    private BigDecimal quantityBefore;

    @Column(precision = 19, scale = 3)
    private BigDecimal quantityAfter;

    @Column(nullable = false, length = 36)
    private String performedBy;

    @Column(nullable = false)
    private Instant performedAt;

    @Column(nullable = false, length = 36)
    private String restaurantId;

    @Column(length = 500)
    private String note;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public UsageActionType getActionType() {
        return actionType;
    }

    public void setActionType(UsageActionType actionType) {
        this.actionType = actionType;
    }

    public BigDecimal getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(BigDecimal quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(BigDecimal quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(BigDecimal quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
