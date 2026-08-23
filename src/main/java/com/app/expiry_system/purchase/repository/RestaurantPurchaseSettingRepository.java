package com.app.expiry_system.purchase.repository;

import com.app.expiry_system.purchase.entity.RestaurantPurchaseSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantPurchaseSettingRepository extends JpaRepository<RestaurantPurchaseSetting, String> {
}
