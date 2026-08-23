package com.app.expiry_system.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class PurchaseSettingRequest {

    @NotNull
    @NotEmpty
    private List<@NotNull DayOfWeek> purchaseDays;

    @NotNull
    @Min(1)
    @Max(30)
    private Integer lookbackPurchaseRuns;

    @NotNull
    private LocalTime notificationTime;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer safetyBufferPercent;

    public List<DayOfWeek> getPurchaseDays() {
        return purchaseDays;
    }

    public void setPurchaseDays(List<DayOfWeek> purchaseDays) {
        this.purchaseDays = purchaseDays;
    }

    public Integer getLookbackPurchaseRuns() {
        return lookbackPurchaseRuns;
    }

    public void setLookbackPurchaseRuns(Integer lookbackPurchaseRuns) {
        this.lookbackPurchaseRuns = lookbackPurchaseRuns;
    }

    public LocalTime getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(LocalTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    public Integer getSafetyBufferPercent() {
        return safetyBufferPercent;
    }

    public void setSafetyBufferPercent(Integer safetyBufferPercent) {
        this.safetyBufferPercent = safetyBufferPercent;
    }
}
