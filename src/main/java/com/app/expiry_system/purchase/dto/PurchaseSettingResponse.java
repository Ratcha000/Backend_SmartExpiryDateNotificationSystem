package com.app.expiry_system.purchase.dto;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class PurchaseSettingResponse {

    private String restaurantId;
    private List<DayOfWeek> purchaseDays;
    private Integer lookbackPurchaseRuns;
    private LocalTime notificationTime;
    private Integer safetyBufferPercent;
    private Instant updatedAt;

    public PurchaseSettingResponse(String restaurantId, List<DayOfWeek> purchaseDays, Integer lookbackPurchaseRuns,
                                   LocalTime notificationTime,
                                   Integer safetyBufferPercent, Instant updatedAt) {
        this.restaurantId = restaurantId;
        this.purchaseDays = purchaseDays;
        this.lookbackPurchaseRuns = lookbackPurchaseRuns;
        this.notificationTime = notificationTime;
        this.safetyBufferPercent = safetyBufferPercent;
        this.updatedAt = updatedAt;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public List<DayOfWeek> getPurchaseDays() {
        return purchaseDays;
    }

    public Integer getLookbackPurchaseRuns() {
        return lookbackPurchaseRuns;
    }

    public LocalTime getNotificationTime() {
        return notificationTime;
    }

    public Integer getSafetyBufferPercent() {
        return safetyBufferPercent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
