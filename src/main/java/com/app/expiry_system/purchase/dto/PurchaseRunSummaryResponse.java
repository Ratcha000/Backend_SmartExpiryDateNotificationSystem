package com.app.expiry_system.purchase.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PurchaseRunSummaryResponse {

    private String runId;
    private String restaurantId;
    private LocalDate runDate;
    private Instant generatedAt;
    private String source;
    private String status;
    private String errorMessage;
    private Integer itemCount;
    private Integer totalBuyItems;
    private List<DayOfWeek> purchaseDays;
    private Integer lookbackPurchaseRuns;
    private Integer safetyBufferPercent;
    private Instant lookbackStartAt;

    public PurchaseRunSummaryResponse(String runId, String restaurantId, LocalDate runDate, Instant generatedAt,
                                      String source, String status, String errorMessage, Integer itemCount,
                                      Integer totalBuyItems, List<DayOfWeek> purchaseDays,
                                      Integer lookbackPurchaseRuns, Integer safetyBufferPercent,
                                      Instant lookbackStartAt) {
        this.runId = runId;
        this.restaurantId = restaurantId;
        this.runDate = runDate;
        this.generatedAt = generatedAt;
        this.source = source;
        this.status = status;
        this.errorMessage = errorMessage;
        this.itemCount = itemCount;
        this.totalBuyItems = totalBuyItems;
        this.purchaseDays = purchaseDays;
        this.lookbackPurchaseRuns = lookbackPurchaseRuns;
        this.safetyBufferPercent = safetyBufferPercent;
        this.lookbackStartAt = lookbackStartAt;
    }

    public String getRunId() {
        return runId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public Integer getTotalBuyItems() {
        return totalBuyItems;
    }

    public List<DayOfWeek> getPurchaseDays() {
        return purchaseDays;
    }

    public Integer getLookbackPurchaseRuns() {
        return lookbackPurchaseRuns;
    }

    public Integer getSafetyBufferPercent() {
        return safetyBufferPercent;
    }

    public Instant getLookbackStartAt() {
        return lookbackStartAt;
    }
}
