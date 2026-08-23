package com.app.expiry_system.ocr.dto;

import jakarta.validation.constraints.NotBlank;

public class OcrScanRequest {

    @NotBlank
    private String restaurantId;

    @NotBlank
    private String rawText;

    private String source = "CAMERA";

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
