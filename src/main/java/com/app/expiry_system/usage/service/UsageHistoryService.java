package com.app.expiry_system.usage.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.ingredient.entity.Ingredient;
import com.app.expiry_system.ingredient.repository.IngredientRepository;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.usage.dto.UsageHistoryRequest;
import com.app.expiry_system.usage.dto.UsageHistoryResponse;
import com.app.expiry_system.usage.entity.UsageHistory;
import com.app.expiry_system.usage.repository.UsageHistoryRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageHistoryService {

    private final UsageHistoryRepository usageHistoryRepository;
    private final IngredientRepository ingredientRepository;
    private final RestaurantRepository restaurantRepository;

    public UsageHistoryService(UsageHistoryRepository usageHistoryRepository,
                               IngredientRepository ingredientRepository,
                               RestaurantRepository restaurantRepository) {
        this.usageHistoryRepository = usageHistoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public UsageHistoryResponse createHistory(UsageHistoryRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(request.getIngredientId(), currentUser);

        UsageHistory history = new UsageHistory();
        history.setIngredientId(ingredient.getId());
        history.setIngredientName(ingredient.getName());
        history.setActionType(request.getActionType());
        history.setQuantityChanged(normalizeNullable(request.getQuantityChanged()));
        history.setUnit(ingredient.getUnit());
        history.setQuantityBefore(normalizeNullable(request.getQuantityBefore()));
        history.setQuantityAfter(normalizeNullable(request.getQuantityAfter()));
        history.setPerformedBy(currentUser.getId());
        history.setRestaurantId(ingredient.getRestaurantId());
        history.setNote(blankToNull(request.getNote()));

        return toResponse(usageHistoryRepository.save(history));
    }

    public List<UsageHistoryResponse> getHistories(String restaurantId, String ingredientId, String actionType, AppUser currentUser) {
        validateManagerRestaurantAccess(restaurantId, currentUser);

        return usageHistoryRepository.findByRestaurantId(restaurantId).stream()
                .filter(history -> ingredientId == null || ingredientId.isBlank() || history.getIngredientId().equals(ingredientId))
                .filter(history -> actionType == null || actionType.isBlank()
                        || history.getActionType().name().equalsIgnoreCase(actionType.trim()))
                .sorted(Comparator.comparing(UsageHistory::getPerformedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UsageHistoryResponse getHistoryById(String id, AppUser currentUser) {
        UsageHistory history = usageHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usage history not found"));
        validateManagerRestaurantAccess(history.getRestaurantId(), currentUser);
        return toResponse(history);
    }

    private Ingredient findAccessibleIngredient(String id, AppUser currentUser) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));
        validateRestaurantAccess(ingredient.getRestaurantId(), currentUser);
        return ingredient;
    }

    private void validateManagerRestaurantAccess(String restaurantId, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only Managers can access usage history reports");
        }
    }

    private void validateRestaurantAccess(String restaurantId, AppUser currentUser) {
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("restaurantId is required");
        }
        if (currentUser.getRestaurantId() == null || currentUser.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("User is not assigned to any restaurant");
        }
        if (!restaurantId.equals(currentUser.getRestaurantId())) {
            throw new IllegalArgumentException("Unauthorized to access this restaurant");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException("Restaurant not found");
        }
    }

    private UsageHistoryResponse toResponse(UsageHistory history) {
        return new UsageHistoryResponse(
                history.getId(),
                history.getIngredientId(),
                history.getIngredientName(),
                history.getActionType(),
                history.getQuantityChanged(),
                history.getUnit(),
                history.getQuantityBefore(),
                history.getQuantityAfter(),
                history.getPerformedBy(),
                history.getPerformedAt(),
                history.getRestaurantId(),
                history.getNote()
        );
    }

    private BigDecimal normalizeNullable(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
