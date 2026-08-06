package com.app.expiry_system.ingredient.dto;

import com.app.expiry_system.ingredient.entity.IngredientStatus;
import jakarta.validation.constraints.NotNull;

public class IngredientStatusUpdateRequest {

    @NotNull
    private IngredientStatus status;

    private String note;

    public IngredientStatus getStatus() {
        return status;
    }

    public void setStatus(IngredientStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
