package com.app.expiry_system.usage.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.usage.dto.UsageHistoryRequest;
import com.app.expiry_system.usage.dto.UsageHistoryResponse;
import com.app.expiry_system.usage.service.UsageHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/usage-history")
@Tag(name = "Usage History")
@SecurityRequirement(name = "bearerAuth")
public class UsageHistoryController {

    private final UsageHistoryService usageHistoryService;

    public UsageHistoryController(UsageHistoryService usageHistoryService) {
        this.usageHistoryService = usageHistoryService;
    }

    @PostMapping
    @Operation(summary = "Create usage history entry", description = "Roles: Manager, Employee")
    public ResponseEntity<UsageHistoryResponse> createHistory(
            @Valid @RequestBody UsageHistoryRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usageHistoryService.createHistory(request, principal.getUser()));
    }

    @GetMapping
    @Operation(summary = "Get usage history by restaurant with optional filters (Manager only)",
            description = "Roles: Manager only")
    public ResponseEntity<List<UsageHistoryResponse>> getHistories(
            @RequestParam String restaurantId,
            @RequestParam(required = false) String ingredientId,
            @RequestParam(required = false) String actionType,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(
                usageHistoryService.getHistories(restaurantId, ingredientId, actionType, principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get usage history entry by ID (Manager only)",
            description = "Roles: Manager only")
    public ResponseEntity<UsageHistoryResponse> getHistoryById(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(usageHistoryService.getHistoryById(id, principal.getUser()));
    }
}
