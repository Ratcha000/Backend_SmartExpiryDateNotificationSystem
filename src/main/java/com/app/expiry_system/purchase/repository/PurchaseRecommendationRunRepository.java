package com.app.expiry_system.purchase.repository;

import com.app.expiry_system.purchase.entity.PurchaseRecommendationRun;
import com.app.expiry_system.purchase.entity.PurchaseRunStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRecommendationRunRepository extends JpaRepository<PurchaseRecommendationRun, String> {

    List<PurchaseRecommendationRun> findByRestaurantIdOrderByGeneratedAtDesc(String restaurantId);

    Optional<PurchaseRecommendationRun> findFirstByRestaurantIdAndStatusOrderByGeneratedAtDesc(
            String restaurantId, PurchaseRunStatus status);
}
