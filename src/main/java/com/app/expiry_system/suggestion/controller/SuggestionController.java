package com.app.expiry_system.suggestion.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.suggestion.dto.IngredientMenuSuggestionResponse;
import com.app.expiry_system.suggestion.dto.MenuSuggestionRequest;
import com.app.expiry_system.suggestion.dto.MenuSuggestionResponse;
import com.app.expiry_system.suggestion.dto.NearExpiryIngredientSuggestionResponse;
import com.app.expiry_system.suggestion.service.SuggestionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suggestions")
@Tag(name = "Suggestions")
@SecurityRequirement(name = "bearerAuth")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping("/menu")
    @Operation(summary = "Suggest menus from selected ingredients", description = "Roles: Manager, Employee")
    public ResponseEntity<MenuSuggestionResponse> suggestMenus(
            @Valid @RequestBody MenuSuggestionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(suggestionService.suggestMenus(request, principal.getUser()));
    }

    @GetMapping("/ingredients/near-expiry")
    @Operation(summary = "Get near-expiry ingredients with menu suggestions", description = "Roles: Manager, Employee")
    public ResponseEntity<List<NearExpiryIngredientSuggestionResponse>> getNearExpirySuggestions(
            @RequestParam String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(suggestionService.getNearExpirySuggestions(restaurantId, principal.getUser()));
    }

    @GetMapping("/menu/{ingredientName}")
    @Operation(summary = "Suggest menus by ingredient name", description = "Roles: Manager, Employee")
    public ResponseEntity<IngredientMenuSuggestionResponse> suggestMenusByIngredient(
            @PathVariable String ingredientName,
            @RequestParam String restaurantId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(suggestionService.suggestMenusByIngredient(restaurantId, ingredientName, principal.getUser()));
    }
}
