package com.app.expiry_system.notification.controller;

import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.notification.dto.NotificationResponse;
import com.app.expiry_system.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get current user notifications", description = "Roles: Manager, Employee")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal.getUser()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Roles: Manager, Employee")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markRead(id, principal.getUser()));
    }
}
