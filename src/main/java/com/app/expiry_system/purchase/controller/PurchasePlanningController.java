package com.app.expiry_system.purchase.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationGenerateRequest;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationResponse;
import com.app.expiry_system.purchase.dto.PurchaseRunDetailResponse;
import com.app.expiry_system.purchase.dto.PurchaseRunSummaryResponse;
import com.app.expiry_system.purchase.dto.PurchaseSettingRequest;
import com.app.expiry_system.purchase.dto.PurchaseSettingResponse;
import com.app.expiry_system.purchase.service.PurchasePlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Purchase Planning")
@SecurityRequirement(name = "bearerAuth")
public class PurchasePlanningController {

    private final PurchasePlanningService purchasePlanningService;

    public PurchasePlanningController(PurchasePlanningService purchasePlanningService) {
        this.purchasePlanningService = purchasePlanningService;
    }

    @GetMapping("/api/purchase-settings/{restaurantId}")
    @Operation(summary = "Get purchase planning settings", description = "Roles: Manager only")
    public ResponseEntity<PurchaseSettingResponse> getSetting(
            @PathVariable String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.getSetting(restaurantId, principal.getUser()));
    }

    @PutMapping("/api/purchase-settings/{restaurantId}")
    @Operation(summary = "Update purchase planning settings", description = "Roles: Manager only")
    public ResponseEntity<PurchaseSettingResponse> updateSetting(
            @PathVariable String restaurantId,
            @Valid @RequestBody PurchaseSettingRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.updateSetting(restaurantId, request, principal.getUser()));
    }

    @GetMapping("/api/purchase-recommendations")
    @Operation(summary = "Get latest purchase recommendations", description = "Roles: Manager only")
    public ResponseEntity<List<PurchaseRecommendationResponse>> getRecommendations(
            @RequestParam String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.getRecommendations(restaurantId, principal.getUser()));
    }

    @GetMapping("/api/purchase-recommendations/runs")
    @Operation(summary = "Get purchase recommendation run history",
            description = "Roles: Manager only. Returns one summary per generate round, newest first.")
    public ResponseEntity<List<PurchaseRunSummaryResponse>> getRuns(
            @RequestParam String restaurantId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.getRuns(restaurantId, limit, principal.getUser()));
    }

    @GetMapping("/api/purchase-recommendations/runs/{runId}")
    @Operation(summary = "Get one purchase recommendation run with its items",
            description = "Roles: Manager only. Only runs that belong to the caller's restaurant are accessible.")
    public ResponseEntity<PurchaseRunDetailResponse> getRunDetail(
            @PathVariable String runId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.getRunDetail(runId, principal.getUser()));
    }

    @PostMapping("/api/purchase-recommendations/generate")
    @Operation(summary = "Generate purchase recommendations", description = "Roles: Manager only")
    public ResponseEntity<List<PurchaseRecommendationResponse>> generateRecommendations(
            @Valid @RequestBody PurchaseRecommendationGenerateRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(purchasePlanningService.generateRecommendations(request, principal.getUser()));
    }
}
