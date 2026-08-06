package com.app.expiry_system.ingredient.repository;

import com.app.expiry_system.ingredient.entity.Ingredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    List<Ingredient> findByRestaurantId(String restaurantId);
}
