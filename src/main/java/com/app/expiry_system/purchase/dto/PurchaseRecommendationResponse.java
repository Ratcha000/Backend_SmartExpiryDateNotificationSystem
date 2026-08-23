package com.app.expiry_system.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PurchaseRecommendationResponse {

    private String id;
    private String restaurantId;
    private String runId;
    private String ingredientName;
    private String category;
    private String unit;
    private BigDecimal currentQuantity;
    private BigDecimal averageDailyUsage;
    private BigDecimal estimatedConsumptionUntilNextCycle;
    private BigDecimal recommendedBuyQuantity;
    private String reason;
    private String confidence;
    private Instant generatedAt;

    public PurchaseRecommendationResponse(String id, String restaurantId, String runId, String ingredientName,
                                          String category, String unit, BigDecimal currentQuantity,
                                          BigDecimal averageDailyUsage,
                                          BigDecimal estimatedConsumptionUntilNextCycle,
                                          BigDecimal recommendedBuyQuantity, String reason, String confidence,
                                          Instant generatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.runId = runId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.unit = unit;
        this.currentQuantity = currentQuantity;
        this.averageDailyUsage = averageDailyUsage;
        this.estimatedConsumptionUntilNextCycle = estimatedConsumptionUntilNextCycle;
        this.recommendedBuyQuantity = recommendedBuyQuantity;
        this.reason = reason;
        this.confidence = confidence;
        this.generatedAt = generatedAt;
    }

    public String getId() {
        return id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public String getRunId() {
        return runId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getAverageDailyUsage() {
        return averageDailyUsage;
    }

    public BigDecimal getEstimatedConsumptionUntilNextCycle() {
        return estimatedConsumptionUntilNextCycle;
    }

    public BigDecimal getRecommendedBuyQuantity() {
        return recommendedBuyQuantity;
    }

    public String getReason() {
        return reason;
    }

    public String getConfidence() {
        return confidence;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
