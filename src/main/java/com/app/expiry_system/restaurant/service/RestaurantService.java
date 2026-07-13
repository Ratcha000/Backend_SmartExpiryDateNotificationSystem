package com.app.expiry_system.restaurant.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.restaurant.dto.JoinRestaurantRequest;
import com.app.expiry_system.restaurant.dto.RestaurantRequest;
import com.app.expiry_system.restaurant.dto.RestaurantResponse;
import com.app.expiry_system.restaurant.entity.Restaurant;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final AppUserRepository appUserRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, AppUserRepository appUserRepository) {
        this.restaurantRepository = restaurantRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request, AppUser manager) {
        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only Managers can create a restaurant");
        }

        if (manager.getRestaurantId() != null && !manager.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("Manager already belongs to a restaurant");
        }
        if (restaurantRepository.findByManagerId(manager.getId()).isPresent()) {
            throw new IllegalArgumentException("Manager already manages a restaurant");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName().trim());
        restaurant.setManagerId(manager.getId());
        restaurant.setInviteCode(generateUniqueInviteCode());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        manager.setRestaurantId(savedRestaurant.getId());
        appUserRepository.save(manager);

        return toResponse(savedRestaurant);
    }

    public RestaurantResponse getRestaurant(String id, AppUser currentUser) {
        Restaurant restaurant = findRestaurantById(id);
        validateRestaurantAccess(restaurant, currentUser);
        return toResponse(restaurant);
    }

    public RestaurantResponse getMyRestaurant(AppUser currentUser) {
        String restaurantId = currentUser.getRestaurantId();
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("User is not assigned to any restaurant");
        }
        Restaurant restaurant = findRestaurantById(restaurantId);
        validateRestaurantAccess(restaurant, currentUser);
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(String id, RestaurantRequest request, AppUser currentUser) {
        Restaurant restaurant = findRestaurantById(id);

        if (!restaurant.getManagerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only the manager can update the restaurant settings");
        }

        restaurant.setName(request.getName().trim());
        Restaurant updated = restaurantRepository.save(restaurant);
        return toResponse(updated);
    }

    public RestaurantResponse getByInviteCode(String inviteCode) {
        Restaurant restaurant = restaurantRepository.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse joinRestaurant(JoinRestaurantRequest request, AppUser currentUser) {
        if (currentUser.getRole() != UserRole.EMPLOYEE) {
            throw new IllegalArgumentException("Only Employees can join a restaurant with an invite code");
        }
        if (currentUser.getRestaurantId() != null && !currentUser.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("User already belongs to a restaurant");
        }

        Restaurant restaurant = restaurantRepository.findByInviteCode(request.getInviteCode().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        currentUser.setRestaurantId(restaurant.getId());
        appUserRepository.save(currentUser);

        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse regenerateInviteCode(String id, AppUser currentUser) {
        Restaurant restaurant = findRestaurantById(id);

        if (!restaurant.getManagerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only the manager can regenerate the invite code");
        }

        restaurant.setInviteCode(generateUniqueInviteCode());
        Restaurant updated = restaurantRepository.save(restaurant);
        return toResponse(updated);
    }

    public List<AppUser> getMembers(String id, AppUser currentUser) {
        Restaurant restaurant = findRestaurantById(id);
        validateRestaurantAccess(restaurant, currentUser);
        return appUserRepository.findByRestaurantId(id);
    }

    private Restaurant findRestaurantById(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
    }

    private void validateRestaurantAccess(Restaurant restaurant, AppUser currentUser) {
        String restaurantId = currentUser.getRestaurantId();
        if (restaurantId == null || restaurantId.isBlank() || !restaurant.getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Unauthorized to access this restaurant");
        }
    }

    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = Restaurant.generateInviteCode();
        } while (restaurantRepository.existsByInviteCode(inviteCode));
        return inviteCode;
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getManagerId(),
                restaurant.getInviteCode(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }
}
