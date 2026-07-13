package com.app.expiry_system.restaurant.controller;

import com.app.expiry_system.auth.dto.UserResponse;
import com.app.expiry_system.auth.security.AppUserPrincipal;
import com.app.expiry_system.restaurant.dto.JoinRestaurantRequest;
import com.app.expiry_system.restaurant.dto.RestaurantRequest;
import com.app.expiry_system.restaurant.dto.RestaurantResponse;
import com.app.expiry_system.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@Tag(name = "Restaurants")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @Operation(summary = "Create a new restaurant (Managers only)")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(request, principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant details by ID")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(restaurantService.getRestaurant(id, principal.getUser()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's restaurant")
    public ResponseEntity<RestaurantResponse> getMyRestaurant(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(restaurantService.getMyRestaurant(principal.getUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update restaurant details (Managers only)")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable String id,
            @Valid @RequestBody RestaurantRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request, principal.getUser()));
    }

    @GetMapping("/invite/{inviteCode}")
    @Operation(summary = "Get restaurant details by invite code")
    public ResponseEntity<RestaurantResponse> getByInviteCode(@PathVariable String inviteCode) {
        return ResponseEntity.ok(restaurantService.getByInviteCode(inviteCode));
    }

    @PostMapping("/join")
    @Operation(summary = "Join a restaurant using invite code")
    public ResponseEntity<RestaurantResponse> joinRestaurant(
            @Valid @RequestBody JoinRestaurantRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(restaurantService.joinRestaurant(request, principal.getUser()));
    }

    @PostMapping("/{id}/invite-code")
    @Operation(summary = "Regenerate invite code for the restaurant (Managers only)")
    public ResponseEntity<RestaurantResponse> regenerateInviteCode(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(restaurantService.regenerateInviteCode(id, principal.getUser()));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get all members/staff of the restaurant")
    public ResponseEntity<List<UserResponse>> getMembers(
            @PathVariable String id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        List<UserResponse> members = restaurantService.getMembers(id, principal.getUser()).stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.getRestaurantId()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }
}
