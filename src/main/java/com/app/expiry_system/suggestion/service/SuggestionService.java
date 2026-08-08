package com.app.expiry_system.suggestion.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.ingredient.entity.Ingredient;
import com.app.expiry_system.ingredient.entity.IngredientStatus;
import com.app.expiry_system.ingredient.repository.IngredientRepository;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.suggestion.dto.IngredientMenuSuggestionResponse;
import com.app.expiry_system.suggestion.dto.MenuSuggestionRequest;
import com.app.expiry_system.suggestion.dto.MenuSuggestionResponse;
import com.app.expiry_system.suggestion.dto.NearExpiryIngredientSuggestionResponse;
import com.app.expiry_system.suggestion.dto.SuggestedMenuResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SuggestionService {

    private static final int DEFAULT_MAX_MENUS = 5;
    private static final int NEAR_EXPIRY_MENU_LIMIT = 3;

    private final IngredientRepository ingredientRepository;
    private final RestaurantRepository restaurantRepository;
    private final KkuAiClient kkuAiClient;

    public SuggestionService(IngredientRepository ingredientRepository,
                             RestaurantRepository restaurantRepository,
                             KkuAiClient kkuAiClient) {
        this.ingredientRepository = ingredientRepository;
        this.restaurantRepository = restaurantRepository;
        this.kkuAiClient = kkuAiClient;
    }

    public MenuSuggestionResponse suggestMenus(MenuSuggestionRequest request, AppUser currentUser) {
        validateRestaurantAccess(request.getRestaurantId(), currentUser);

        List<Ingredient> stock = getAvailableStock(request.getRestaurantId());
        List<String> sourceIngredients = normalizeNames(request.getIngredientNames());
        List<SuggestedMenuResponse> menus = callAi(sourceIngredients, stock, resolveMaxMenus(request.getMaxMenus()),
                resolveLanguage(request.getLanguage()));

        return new MenuSuggestionResponse(request.getRestaurantId(), sourceIngredients, menus);
    }

    public IngredientMenuSuggestionResponse suggestMenusByIngredient(String restaurantId, String ingredientName,
                                                                     AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
        if (ingredientName == null || ingredientName.isBlank()) {
            throw new IllegalArgumentException("ingredientName is required");
        }

        List<Ingredient> stock = getAvailableStock(restaurantId);
        String normalizedIngredientName = ingredientName.trim();
        List<SuggestedMenuResponse> menus = callAi(List.of(normalizedIngredientName), stock, DEFAULT_MAX_MENUS, "th");

        return new IngredientMenuSuggestionResponse(restaurantId, normalizedIngredientName, menus);
    }

    public List<NearExpiryIngredientSuggestionResponse> getNearExpirySuggestions(String restaurantId,
                                                                                 AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);

        List<Ingredient> stock = getAvailableStock(restaurantId);
        return stock.stream()
                .filter(ingredient -> ingredient.getStatus() == IngredientStatus.ACTIVE)
                .filter(this::isNearExpiry)
                .sorted(Comparator.comparing(Ingredient::getExpiryDate)
                        .thenComparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER))
                .map(ingredient -> new NearExpiryIngredientSuggestionResponse(
                        ingredient.getId(),
                        ingredient.getName(),
                        ingredient.getCategory(),
                        ingredient.getQuantity(),
                        ingredient.getUnit(),
                        ingredient.getExpiryDate(),
                        daysLeft(ingredient),
                        callAi(List.of(ingredient.getName()), stock, NEAR_EXPIRY_MENU_LIMIT, "th")
                ))
                .collect(Collectors.toList());
    }

    private List<SuggestedMenuResponse> callAi(List<String> sourceIngredients, List<Ingredient> stock,
                                               int maxMenus, String language) {
        String systemPrompt = """
                You are a restaurant inventory menu suggestion assistant.
                Return only valid JSON. Do not include markdown, comments, or extra text.
                The JSON schema must be:
                {
                  "menus": [
                    {
                      "menuName": "string",
                      "description": "string",
                      "ingredientsRequired": ["string"],
                      "ingredientsInStock": ["string"],
                      "missingIngredients": ["string"],
                      "steps": ["string"],
                      "priority": "HIGH|MEDIUM|LOW",
                      "reason": "string"
                    }
                  ]
                }
                """;

        String userPrompt = """
                Suggest up to %d practical restaurant menu ideas in language: %s.
                Prioritize using these source ingredients before they expire: %s.
                Current restaurant stock:
                %s
                Rules:
                - You must return at least 1 menu when source ingredients are provided.
                - Do not return an empty menus array.
                - ingredientsInStock must only include items from current restaurant stock.
                - missingIngredients should include common items needed but not found in stock.
                - Prefer simple menus that a restaurant kitchen can actually prepare.
                - priority should be HIGH when the source ingredient should be used urgently.
                """.formatted(maxMenus, language, String.join(", ", sourceIngredients), formatStock(stock));

        return kkuAiClient.suggestMenus(systemPrompt, userPrompt);
    }

    private String formatStock(List<Ingredient> stock) {
        return stock.stream()
                .map(ingredient -> "- %s: %s %s, category=%s, expiryDate=%s, daysLeft=%d"
                        .formatted(ingredient.getName(), ingredient.getQuantity(), ingredient.getUnit(),
                                ingredient.getCategory(), ingredient.getExpiryDate(), daysLeft(ingredient)))
                .collect(Collectors.joining("\n"));
    }

    private List<Ingredient> getAvailableStock(String restaurantId) {
        return ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(ingredient -> ingredient.getStatus() != IngredientStatus.DELETED)
                .filter(ingredient -> ingredient.getQuantity().signum() > 0)
                .collect(Collectors.toList());
    }

    private List<String> normalizeNames(List<String> names) {
        return names.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .collect(Collectors.toList());
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

    private boolean isNearExpiry(Ingredient ingredient) {
        long daysLeft = daysLeft(ingredient);
        return daysLeft >= 0 && daysLeft <= ingredient.getNotifyDaysBefore();
    }

    private long daysLeft(Ingredient ingredient) {
        return ChronoUnit.DAYS.between(LocalDate.now(), ingredient.getExpiryDate());
    }

    private int resolveMaxMenus(Integer maxMenus) {
        return maxMenus == null ? DEFAULT_MAX_MENUS : maxMenus;
    }

    private String resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "th";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }
}
