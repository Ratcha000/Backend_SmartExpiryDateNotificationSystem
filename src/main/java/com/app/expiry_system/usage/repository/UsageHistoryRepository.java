package com.app.expiry_system.usage.repository;

import com.app.expiry_system.usage.entity.UsageHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageHistoryRepository extends JpaRepository<UsageHistory, String> {

    List<UsageHistory> findByIngredientId(String ingredientId);

    List<UsageHistory> findByRestaurantId(String restaurantId);
}
