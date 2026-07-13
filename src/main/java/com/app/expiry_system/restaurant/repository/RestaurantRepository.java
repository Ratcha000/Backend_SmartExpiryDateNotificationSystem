package com.app.expiry_system.restaurant.repository;

import com.app.expiry_system.restaurant.entity.Restaurant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
    Optional<Restaurant> findByInviteCode(String inviteCode);
    Optional<Restaurant> findByManagerId(String managerId);
    boolean existsByInviteCode(String inviteCode);
}
