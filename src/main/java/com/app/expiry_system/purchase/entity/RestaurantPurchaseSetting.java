package com.app.expiry_system.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "restaurant_purchase_settings")
public class RestaurantPurchaseSetting {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String restaurantId;

    @Column(nullable = false, length = 120)
    private String purchaseDays;

    @Column(name = "buy_cycle_days")
    private Integer legacyBuyCycleDays;

    @Column(name = "next_planned_purchase_date")
    private LocalDate legacyNextPlannedPurchaseDate;

    @Column(nullable = false)
    private Integer lookbackPurchaseRuns;

    @Column(nullable = false)
    private LocalTime notificationTime;

    @Column(nullable = false)
    private Integer safetyBufferPercent;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (legacyBuyCycleDays == null) {
            legacyBuyCycleDays = 7;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getPurchaseDays() {
        return purchaseDays;
    }

    public void setPurchaseDays(String purchaseDays) {
        this.purchaseDays = purchaseDays;
    }

    public Integer getLegacyBuyCycleDays() {
        return legacyBuyCycleDays;
    }

    public void setLegacyBuyCycleDays(Integer legacyBuyCycleDays) {
        this.legacyBuyCycleDays = legacyBuyCycleDays;
    }

    public LocalDate getLegacyNextPlannedPurchaseDate() {
        return legacyNextPlannedPurchaseDate;
    }

    public void setLegacyNextPlannedPurchaseDate(LocalDate legacyNextPlannedPurchaseDate) {
        this.legacyNextPlannedPurchaseDate = legacyNextPlannedPurchaseDate;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
