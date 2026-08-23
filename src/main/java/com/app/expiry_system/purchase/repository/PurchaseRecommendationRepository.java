package com.app.expiry_system.purchase.repository;

import com.app.expiry_system.purchase.entity.PurchaseRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRecommendationRepository extends JpaRepository<PurchaseRecommendation, String> {

    List<PurchaseRecommendation> findByRestaurantId(String restaurantId);

    List<PurchaseRecommendation> findByRunId(String runId);

    void deleteByRestaurantId(String restaurantId);

    void deleteByRunIdIn(List<String> runIds);

    void deleteByRestaurantIdAndRunIdIsNull(String restaurantId);
}
