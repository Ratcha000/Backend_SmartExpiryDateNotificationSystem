package com.app.expiry_system.ingredient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String restaurantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 36)
    private String lotId;

    @Column(length = 120)
    private String lotName;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal initialQuantity;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(length = 50)
    private String categoryUnitHint;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Integer notifyDaysBefore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientStatus status;

    @Column(length = 36)
    private String scannedBy;

    private Instant scannedAt;

    private Instant lastUsedAt;

    @Column(length = 36)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = IngredientStatus.ACTIVE;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(BigDecimal initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategoryUnitHint() {
        return categoryUnitHint;
    }

    public void setCategoryUnitHint(String categoryUnitHint) {
        this.categoryUnitHint = categoryUnitHint;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getNotifyDaysBefore() {
        return notifyDaysBefore;
    }

    public void setNotifyDaysBefore(Integer notifyDaysBefore) {
        this.notifyDaysBefore = notifyDaysBefore;
    }

    public IngredientStatus getStatus() {
        return status;
    }

    public void setStatus(IngredientStatus status) {
        this.status = status;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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
