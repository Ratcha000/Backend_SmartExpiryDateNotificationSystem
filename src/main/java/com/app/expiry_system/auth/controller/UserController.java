package com.app.expiry_system.auth.controller;

import com.app.expiry_system.auth.dto.UserResponse;
import com.app.expiry_system.auth.dto.UserUpdateRequest;
import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.toResponse(principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(userService.updateUser(id, request, principal.getUser()));
    }

    @GetMapping
    @Operation(summary = "Get users (optionally filter by restaurantId)")
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(required = false) String restaurantId) {
        return ResponseEntity.ok(userService.getUsers(restaurantId));
    }
}
