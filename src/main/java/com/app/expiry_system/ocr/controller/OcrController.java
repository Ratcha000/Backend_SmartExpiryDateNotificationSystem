package com.app.expiry_system.ocr.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.ocr.dto.OcrScanRequest;
import com.app.expiry_system.ocr.dto.OcrScanResponse;
import com.app.expiry_system.ocr.service.OcrScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@Tag(name = "OCR / Scan Support")
@SecurityRequirement(name = "bearerAuth")
public class OcrController {

    private final OcrScanService ocrScanService;

    public OcrController(OcrScanService ocrScanService) {
        this.ocrScanService = ocrScanService;
    }

    @PostMapping("/extract-expiry-date")
    @Operation(summary = "Extract expiry date from OCR text", description = "Roles: Manager, Employee")
    public ResponseEntity<OcrScanResponse> extractExpiryDate(
            @Valid @RequestBody OcrScanRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ocrScanService.extractExpiryDate(request, principal.getUser()));
    }

    @PostMapping("/scan")
    @Operation(summary = "Parse OCR scan text", description = "Roles: Manager, Employee")
    public ResponseEntity<OcrScanResponse> scan(
            @Valid @RequestBody OcrScanRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ocrScanService.scan(request, principal.getUser()));
    }

    @PostMapping(value = "/scan-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image for OCR scanning", description = "Roles: Manager, Employee")
    public ResponseEntity<OcrScanResponse> scanImage(
            @RequestParam String restaurantId,
            @RequestParam MultipartFile image,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        ocrScanService.validateImageScanAccess(restaurantId, principal.getUser());
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("image is required");
        }
        throw new IllegalArgumentException("Backend image OCR is not enabled yet. Send OCR text to /api/ocr/extract-expiry-date.");
    }
}
