package com.app.expiry_system.ingredient.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.ingredient.dto.IngredientBatchRequest;
import com.app.expiry_system.ingredient.dto.IngredientRequest;
import com.app.expiry_system.ingredient.dto.IngredientResponse;
import com.app.expiry_system.ingredient.dto.IngredientStatusUpdateRequest;
import com.app.expiry_system.ingredient.dto.NoteRequest;
import com.app.expiry_system.ingredient.dto.QuantityAdjustmentRequest;
import com.app.expiry_system.ingredient.dto.QuantityChangeRequest;
import com.app.expiry_system.ingredient.entity.IngredientStatus;
import com.app.expiry_system.ingredient.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
@Tag(name = "Ingredients")
@SecurityRequirement(name = "bearerAuth")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    @Operation(summary = "Create a new ingredient", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> createIngredient(
            @Valid @RequestBody IngredientRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.createIngredient(request, principal.getUser()));
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple ingredients in the same lot", description = "Roles: Manager, Employee")
    public ResponseEntity<List<IngredientResponse>> createIngredientsBatch(
            @Valid @RequestBody IngredientBatchRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.createIngredientsBatch(request, principal.getUser()));
    }

    @GetMapping
    @Operation(summary = "Get ingredients by restaurant and optional filters", description = "Roles: Manager, Employee")
    public ResponseEntity<List<IngredientResponse>> getIngredients(
            @RequestParam String restaurantId,
            @RequestParam(required = false) IngredientStatus status,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.getIngredients(restaurantId, status, category, principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ingredient by ID", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> getIngredient(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.getIngredient(id, principal.getUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ingredient details", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> updateIngredient(
            @PathVariable String id,
            @Valid @RequestBody IngredientRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.updateIngredient(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/consume")
    @Operation(summary = "Consume ingredient quantity", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> consumeIngredient(
            @PathVariable String id,
            @Valid @RequestBody QuantityChangeRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.consumeIngredient(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/restock")
    @Operation(summary = "Restock ingredient quantity", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> restockIngredient(
            @PathVariable String id,
            @Valid @RequestBody QuantityChangeRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.restockIngredient(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/adjust-quantity")
    @Operation(summary = "Adjust ingredient quantity directly (Manager only)",
            description = "Roles: Manager only")
    public ResponseEntity<IngredientResponse> adjustQuantity(
            @PathVariable String id,
            @Valid @RequestBody QuantityAdjustmentRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.adjustQuantity(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/used")
    @Operation(summary = "Mark ingredient as used", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> markUsed(
            @PathVariable String id,
            @RequestBody(required = false) NoteRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.markUsed(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/delete")
    @Operation(summary = "Soft delete ingredient", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> deleteIngredient(
            @PathVariable String id,
            @RequestBody(required = false) NoteRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.deleteIngredient(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ingredient status", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody IngredientStatusUpdateRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.updateStatus(id, request, principal.getUser()));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring ingredients", description = "Roles: Manager, Employee")
    public ResponseEntity<List<IngredientResponse>> getExpiringIngredients(
            @RequestParam String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.getExpiringIngredients(restaurantId, principal.getUser()));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired ingredients", description = "Roles: Manager, Employee")
    public ResponseEntity<List<IngredientResponse>> getExpiredIngredients(
            @RequestParam String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.getExpiredIngredients(restaurantId, principal.getUser()));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low-stock ingredients", description = "Roles: Manager, Employee")
    public ResponseEntity<List<IngredientResponse>> getLowStockIngredients(
            @RequestParam String restaurantId,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ingredientService.getLowStockIngredients(restaurantId, category, principal.getUser()));
    }
}
