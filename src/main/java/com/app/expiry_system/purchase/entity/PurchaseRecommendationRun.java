package com.app.expiry_system.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_recommendation_runs",
        indexes = @Index(name = "idx_purchase_run_restaurant", columnList = "restaurantId"))
public class PurchaseRecommendationRun {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String restaurantId;

    @Column(nullable = false)
    private LocalDate runDate;

    @Column(nullable = false)
    private Instant generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRunSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseRunStatus status;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private Integer itemCount;

    @Column(nullable = false)
    private Integer totalBuyItems;

    @Column(nullable = false, length = 120)
    private String purchaseDays;

    @Column(nullable = false)
    private Integer lookbackPurchaseRuns;

    @Column(nullable = false)
    private Integer safetyBufferPercent;

    @Column(nullable = false)
    private Instant lookbackStartAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
        if (itemCount == null) {
            itemCount = 0;
        }
        if (totalBuyItems == null) {
            totalBuyItems = 0;
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

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public PurchaseRunSource getSource() {
        return source;
    }

    public void setSource(PurchaseRunSource source) {
        this.source = source;
    }

    public PurchaseRunStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseRunStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getTotalBuyItems() {
        return totalBuyItems;
    }

    public void setTotalBuyItems(Integer totalBuyItems) {
        this.totalBuyItems = totalBuyItems;
    }

    public String getPurchaseDays() {
        return purchaseDays;
    }

    public void setPurchaseDays(String purchaseDays) {
        this.purchaseDays = purchaseDays;
    }

    public Integer getLookbackPurchaseRuns() {
        return lookbackPurchaseRuns;
    }

    public void setLookbackPurchaseRuns(Integer lookbackPurchaseRuns) {
        this.lookbackPurchaseRuns = lookbackPurchaseRuns;
    }

    public Integer getSafetyBufferPercent() {
        return safetyBufferPercent;
    }

    public void setSafetyBufferPercent(Integer safetyBufferPercent) {
        this.safetyBufferPercent = safetyBufferPercent;
    }

    public Instant getLookbackStartAt() {
        return lookbackStartAt;
    }

    public void setLookbackStartAt(Instant lookbackStartAt) {
        this.lookbackStartAt = lookbackStartAt;
    }
}
