package com.app.expiry_system.ingredient.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.ingredient.dto.IngredientBatchItemRequest;
import com.app.expiry_system.ingredient.dto.IngredientBatchRequest;
import com.app.expiry_system.ingredient.dto.IngredientRequest;
import com.app.expiry_system.ingredient.dto.IngredientResponse;
import com.app.expiry_system.ingredient.dto.IngredientStatusUpdateRequest;
import com.app.expiry_system.ingredient.dto.NoteRequest;
import com.app.expiry_system.ingredient.dto.QuantityAdjustmentRequest;
import com.app.expiry_system.ingredient.dto.QuantityChangeRequest;
import com.app.expiry_system.ingredient.entity.Ingredient;
import com.app.expiry_system.ingredient.entity.IngredientStatus;
import com.app.expiry_system.ingredient.repository.IngredientRepository;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.usage.entity.UsageActionType;
import com.app.expiry_system.usage.entity.UsageHistory;
import com.app.expiry_system.usage.repository.UsageHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {

    private static final BigDecimal LOW_STOCK_THRESHOLD_RATIO = new BigDecimal("0.20");

    private final IngredientRepository ingredientRepository;
    private final UsageHistoryRepository usageHistoryRepository;
    private final RestaurantRepository restaurantRepository;

    public IngredientService(IngredientRepository ingredientRepository,
                             UsageHistoryRepository usageHistoryRepository,
                             RestaurantRepository restaurantRepository) {
        this.ingredientRepository = ingredientRepository;
        this.usageHistoryRepository = usageHistoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public IngredientResponse createIngredient(IngredientRequest request, AppUser currentUser) {
        validateRestaurantAccess(request.getRestaurantId(), currentUser);
        validateIngredientRequest(request);

        Ingredient ingredient = new Ingredient();
        applyRequest(ingredient, request);
        ingredient.setUpdatedBy(currentUser.getId());
        ingredient.setStatus(resolveStatusFromState(request.getExpiryDate(), request.getQuantity()));

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.ADDED, null, saved.getQuantity(), saved.getQuantity(), null, currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public List<IngredientResponse> createIngredientsBatch(IngredientBatchRequest request, AppUser currentUser) {
        validateRestaurantAccess(request.getRestaurantId(), currentUser);
        request.getItems().forEach(this::validateBatchItemRequest);

        String lotId = UUID.randomUUID().toString();
        String lotName = request.getLotName().trim();

        return request.getItems().stream()
                .map(item -> {
                    Ingredient ingredient = new Ingredient();
                    applyBatchRequest(ingredient, request, item, lotId, lotName);
                    ingredient.setUpdatedBy(currentUser.getId());
                    ingredient.setStatus(resolveStatusFromState(request.getExpiryDate(), item.getQuantity()));

                    Ingredient saved = ingredientRepository.save(ingredient);
                    logUsage(saved, UsageActionType.ADDED, null, saved.getQuantity(), saved.getQuantity(), null, currentUser.getId());
                    return toResponse(saved);
                })
                .collect(Collectors.toList());
    }

    public List<IngredientResponse> getIngredients(String restaurantId, IngredientStatus status, String category, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        return ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(ingredient -> status == null || ingredient.getStatus() == status)
                .filter(ingredient -> category == null || category.isBlank()
                        || ingredient.getCategory().equalsIgnoreCase(category.trim()))
                .sorted(Comparator.comparing(Ingredient::getExpiryDate).thenComparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public IngredientResponse getIngredient(String id, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse updateIngredient(String id, IngredientRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        if (!ingredient.getRestaurantId().equals(request.getRestaurantId())) {
            throw new IllegalArgumentException("Ingredient restaurantId cannot be changed");
        }
        validateIngredientRequest(request);

        BigDecimal before = ingredient.getQuantity();
        applyRequest(ingredient, request);
        ingredient.setUpdatedBy(currentUser.getId());
        ingredient.setStatus(resolveStatusFromState(ingredient.getExpiryDate(), ingredient.getQuantity()));

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.EDITED, before, saved.getQuantity(), null, null, currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse consumeIngredient(String id, QuantityChangeRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        ensureActionableIngredient(ingredient);

        BigDecimal before = ingredient.getQuantity();
        if (request.getQuantity().compareTo(before) > 0) {
            throw new IllegalArgumentException("Consume quantity cannot exceed remaining quantity");
        }

        BigDecimal after = before.subtract(request.getQuantity());
        ingredient.setQuantity(after);
        ingredient.setLastUsedAt(Instant.now());
        ingredient.setUpdatedBy(currentUser.getId());
        ingredient.setStatus(resolveStatusFromState(ingredient.getExpiryDate(), after));

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.CONSUMED, before, after, request.getQuantity(), request.getNote(), currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse restockIngredient(String id, QuantityChangeRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        if (ingredient.getStatus() == IngredientStatus.DELETED) {
            throw new IllegalArgumentException("Deleted ingredient cannot be restocked");
        }

        BigDecimal before = ingredient.getQuantity();
        BigDecimal after = before.add(request.getQuantity());
        ingredient.setQuantity(after);
        ingredient.setUpdatedBy(currentUser.getId());
        ingredient.setStatus(resolveStatusFromState(ingredient.getExpiryDate(), after));

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.RESTOCKED, before, after, request.getQuantity(), request.getNote(), currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse adjustQuantity(String id, QuantityAdjustmentRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only Managers can adjust ingredient quantity directly");
        }

        BigDecimal before = ingredient.getQuantity();
        BigDecimal after = request.getQuantity();
        ingredient.setQuantity(after);
        ingredient.setUpdatedBy(currentUser.getId());
        ingredient.setStatus(resolveStatusFromState(ingredient.getExpiryDate(), after));

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.ADJUSTED, before, after, after.subtract(before).abs(), request.getReason(), currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse markUsed(String id, NoteRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        BigDecimal before = ingredient.getQuantity();
        ingredient.setQuantity(BigDecimal.ZERO);
        ingredient.setStatus(resolveStatusFromState(ingredient.getExpiryDate(), BigDecimal.ZERO));
        ingredient.setLastUsedAt(Instant.now());
        ingredient.setUpdatedBy(currentUser.getId());

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.USED, before, BigDecimal.ZERO, before, noteOrNull(request), currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse deleteIngredient(String id, NoteRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        ingredient.setStatus(IngredientStatus.DELETED);
        ingredient.setUpdatedBy(currentUser.getId());

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.DELETED, saved.getQuantity(), saved.getQuantity(), null, noteOrNull(request), currentUser.getId());
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse updateStatus(String id, IngredientStatusUpdateRequest request, AppUser currentUser) {
        Ingredient ingredient = findAccessibleIngredient(id, currentUser);
        validateStatusChangeConsistency(request.getStatus(), ingredient.getQuantity());
        ingredient.setStatus(request.getStatus());
        if (request.getStatus() == IngredientStatus.USED) {
            ingredient.setQuantity(BigDecimal.ZERO);
            ingredient.setLastUsedAt(Instant.now());
        }
        ingredient.setUpdatedBy(currentUser.getId());

        Ingredient saved = ingredientRepository.save(ingredient);
        logUsage(saved, UsageActionType.EDITED, saved.getQuantity(), saved.getQuantity(), null, request.getNote(), currentUser.getId());
        return toResponse(saved);
    }

    public List<IngredientResponse> getExpiringIngredients(String restaurantId, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        return ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(ingredient -> ingredient.getStatus() == IngredientStatus.ACTIVE)
                .filter(this::isExpiring)
                .sorted(Comparator.comparing(Ingredient::getExpiryDate))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<IngredientResponse> getExpiredIngredients(String restaurantId, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        return ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(this::isExpired)
                .sorted(Comparator.comparing(Ingredient::getExpiryDate))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<IngredientResponse> getLowStockIngredients(String restaurantId, String category, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        return ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(ingredient -> ingredient.getStatus() != IngredientStatus.DELETED)
                .filter(ingredient -> category == null || category.isBlank()
                        || ingredient.getCategory().equalsIgnoreCase(category.trim()))
                .filter(this::isLowStock)
                .sorted(Comparator.comparing(Ingredient::getQuantity).thenComparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateIngredientRequest(IngredientRequest request) {
        if (request.getInitialQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("initialQuantity must be greater than or equal to quantity");
        }
        if (request.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Ingredient quantity must be greater than 0 when created or updated");
        }
    }

    private void validateBatchItemRequest(IngredientBatchItemRequest request) {
        if (request.getInitialQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("initialQuantity must be greater than or equal to quantity");
        }
        if (request.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Ingredient quantity must be greater than 0 when created in batch");
        }
    }

    private void applyRequest(Ingredient ingredient, IngredientRequest request) {
        ingredient.setRestaurantId(request.getRestaurantId().trim());
        ingredient.setName(request.getName().trim());
        ingredient.setCategory(request.getCategory().trim().toLowerCase(Locale.ROOT));
        ingredient.setInitialQuantity(normalize(request.getInitialQuantity()));
        ingredient.setQuantity(normalize(request.getQuantity()));
        ingredient.setUnit(request.getUnit().trim());
        ingredient.setCategoryUnitHint(blankToNull(request.getCategoryUnitHint()));
        ingredient.setExpiryDate(request.getExpiryDate());
        ingredient.setNotifyDaysBefore(request.getNotifyDaysBefore());
        ingredient.setScannedBy(blankToNull(request.getScannedBy()));
        ingredient.setScannedAt(request.getScannedAt());
    }

    private void applyBatchRequest(Ingredient ingredient, IngredientBatchRequest request, IngredientBatchItemRequest item,
                                   String lotId, String lotName) {
        String partName = item.getPartName().trim();
        ingredient.setRestaurantId(request.getRestaurantId().trim());
        ingredient.setName(lotName + " - " + partName);
        ingredient.setLotId(lotId);
        ingredient.setLotName(lotName);
        ingredient.setCategory(request.getCategory().trim().toLowerCase(Locale.ROOT));
        ingredient.setInitialQuantity(normalize(item.getInitialQuantity()));
        ingredient.setQuantity(normalize(item.getQuantity()));
        ingredient.setUnit(request.getUnit().trim());
        ingredient.setCategoryUnitHint(blankToNull(request.getCategoryUnitHint()));
        ingredient.setExpiryDate(request.getExpiryDate());
        ingredient.setNotifyDaysBefore(request.getNotifyDaysBefore());
        ingredient.setScannedBy(blankToNull(request.getScannedBy()));
        ingredient.setScannedAt(request.getScannedAt());
    }

    private Ingredient findAccessibleIngredient(String id, AppUser currentUser) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingredient not found"));
        validateRestaurantAccess(ingredient.getRestaurantId(), currentUser);
        return ingredient;
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

    private IngredientStatus resolveStatusFromState(LocalDate expiryDate, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return IngredientStatus.USED;
        }
        if (expiryDate.isBefore(LocalDate.now())) {
            return IngredientStatus.EXPIRED;
        }
        return IngredientStatus.ACTIVE;
    }

    private boolean isExpired(Ingredient ingredient) {
        return ingredient.getStatus() == IngredientStatus.EXPIRED || daysLeft(ingredient) < 0;
    }

    private boolean isExpiring(Ingredient ingredient) {
        long daysLeft = daysLeft(ingredient);
        return daysLeft >= 0 && daysLeft <= ingredient.getNotifyDaysBefore();
    }

    private boolean isLowStock(Ingredient ingredient) {
        if (ingredient.getInitialQuantity() == null || ingredient.getInitialQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal threshold = ingredient.getInitialQuantity().multiply(LOW_STOCK_THRESHOLD_RATIO);
        return ingredient.getQuantity().compareTo(threshold) <= 0;
    }

    private long daysLeft(Ingredient ingredient) {
        return ChronoUnit.DAYS.between(LocalDate.now(), ingredient.getExpiryDate());
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        long daysLeft = daysLeft(ingredient);
        boolean expired = ingredient.getStatus() == IngredientStatus.EXPIRED || daysLeft < 0;
        boolean expiring = ingredient.getStatus() == IngredientStatus.ACTIVE
                && !expired
                && daysLeft <= ingredient.getNotifyDaysBefore();

        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getRestaurantId(),
                ingredient.getName(),
                ingredient.getLotId(),
                ingredient.getLotName(),
                ingredient.getCategory(),
                ingredient.getInitialQuantity(),
                ingredient.getQuantity(),
                ingredient.getUnit(),
                ingredient.getCategoryUnitHint(),
                ingredient.getExpiryDate(),
                ingredient.getNotifyDaysBefore(),
                ingredient.getStatus(),
                daysLeft,
                expiring,
                expired,
                ingredient.getScannedBy(),
                ingredient.getScannedAt(),
                ingredient.getLastUsedAt(),
                ingredient.getUpdatedBy(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt()
        );
    }

    private void logUsage(Ingredient ingredient, UsageActionType actionType, BigDecimal before, BigDecimal after,
                          BigDecimal changed, String note, String performedBy) {
        UsageHistory history = new UsageHistory();
        history.setIngredientId(ingredient.getId());
        history.setIngredientName(ingredient.getName());
        history.setActionType(actionType);
        history.setQuantityBefore(before == null ? null : normalize(before));
        history.setQuantityAfter(after == null ? null : normalize(after));
        history.setQuantityChanged(changed == null ? null : normalize(changed));
        history.setUnit(ingredient.getUnit());
        history.setPerformedBy(performedBy);
        history.setRestaurantId(ingredient.getRestaurantId());
        history.setNote(blankToNull(note));
        usageHistoryRepository.save(history);
    }

    private void ensureActionableIngredient(Ingredient ingredient) {
        if (ingredient.getStatus() == IngredientStatus.DELETED) {
            throw new IllegalArgumentException("Deleted ingredient cannot be modified");
        }
        if (ingredient.getStatus() == IngredientStatus.USED) {
            throw new IllegalArgumentException("Used ingredient cannot be consumed again");
        }
    }

    private void validateStatusChangeConsistency(IngredientStatus status, BigDecimal quantity) {
        if (status == IngredientStatus.ACTIVE && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Active ingredient must have quantity greater than 0");
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String noteOrNull(NoteRequest request) {
        return request == null ? null : blankToNull(request.getNote());
    }
}
