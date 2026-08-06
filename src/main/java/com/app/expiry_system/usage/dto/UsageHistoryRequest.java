package com.app.expiry_system.usage.dto;

import com.app.expiry_system.usage.entity.UsageActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UsageHistoryRequest {

    @NotBlank
    private String ingredientId;

    @NotNull
    private UsageActionType actionType;

    private BigDecimal quantityChanged;

    private BigDecimal quantityBefore;

    private BigDecimal quantityAfter;

    private String note;

    public String getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
