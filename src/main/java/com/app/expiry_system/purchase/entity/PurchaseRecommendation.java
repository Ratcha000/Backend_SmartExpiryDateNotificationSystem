package com.app.expiry_system.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_recommendations")
public class PurchaseRecommendation {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String restaurantId;

    @Column(length = 36)
    private String runId;

    @Column(nullable = false, length = 120)
    private String ingredientName;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal currentQuantity;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal averageDailyUsage;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal estimatedConsumptionUntilNextCycle;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal recommendedBuyQuantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String confidence;

    @Column(nullable = false)
    private Instant generatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
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

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public BigDecimal getAverageDailyUsage() {
        return averageDailyUsage;
    }

    public void setAverageDailyUsage(BigDecimal averageDailyUsage) {
        this.averageDailyUsage = averageDailyUsage;
    }

    public BigDecimal getEstimatedConsumptionUntilNextCycle() {
        return estimatedConsumptionUntilNextCycle;
    }

    public void setEstimatedConsumptionUntilNextCycle(BigDecimal estimatedConsumptionUntilNextCycle) {
        this.estimatedConsumptionUntilNextCycle = estimatedConsumptionUntilNextCycle;
    }

    public BigDecimal getRecommendedBuyQuantity() {
        return recommendedBuyQuantity;
    }

    public void setRecommendedBuyQuantity(BigDecimal recommendedBuyQuantity) {
        this.recommendedBuyQuantity = recommendedBuyQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
