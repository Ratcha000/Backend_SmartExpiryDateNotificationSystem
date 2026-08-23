package com.app.expiry_system.ocr.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class OcrScanResponse {

    private String rawText;
    private LocalDate expiryDate;
    private String matchedText;
    private String confidence;
    private List<String> warnings;
    private SuggestedIngredientPreview suggestedIngredient;
    private Instant scannedAt;
    private String scannedBy;

    public OcrScanResponse(String rawText, LocalDate expiryDate, String matchedText, String confidence,
                           List<String> warnings, SuggestedIngredientPreview suggestedIngredient,
                           Instant scannedAt, String scannedBy) {
        this.rawText = rawText;
        this.expiryDate = expiryDate;
        this.matchedText = matchedText;
        this.confidence = confidence;
        this.warnings = warnings;
        this.suggestedIngredient = suggestedIngredient;
        this.scannedAt = scannedAt;
        this.scannedBy = scannedBy;
    }

    public String getRawText() {
        return rawText;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public String getConfidence() {
        return confidence;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public SuggestedIngredientPreview getSuggestedIngredient() {
        return suggestedIngredient;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public String getScannedBy() {
        return scannedBy;
    }
}
