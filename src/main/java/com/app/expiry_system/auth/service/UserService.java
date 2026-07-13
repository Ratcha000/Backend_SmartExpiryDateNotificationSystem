package com.app.expiry_system.auth.service;

import com.app.expiry_system.auth.dto.UserResponse;
import com.app.expiry_system.auth.dto.UserUpdateRequest;
import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.repository.AppUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public UserResponse getUserById(String id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request, AppUser currentUser) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Only allow users to update their own profile, OR if the current user is a MANAGER and belongs to the same restaurant
        boolean isSelf = user.getId().equals(currentUser.getId());
        boolean isManagerOfSameRestaurant = currentUser.getRole() == com.app.expiry_system.auth.entity.UserRole.MANAGER 
                && currentUser.getRestaurantId() != null 
                && currentUser.getRestaurantId().equals(user.getRestaurantId());

        if (!isSelf && !isManagerOfSameRestaurant) {
            throw new IllegalArgumentException("Unauthorized to update this user");
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getRole() != null && isSelf) {
            user.setRole(request.getRole());
        }
        if (request.getRestaurantId() != null) {
            throw new IllegalArgumentException("Restaurant assignment must be managed through restaurant APIs");
        }

        AppUser updated = appUserRepository.save(user);
        return toResponse(updated);
    }

    public List<UserResponse> getUsers(String restaurantId) {
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            return appUserRepository.findByRestaurantId(restaurantId.trim()).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return appUserRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getRestaurantId()
        );
    }
}
