package com.app.expiry_system.purchase.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.ingredient.entity.Ingredient;
import com.app.expiry_system.ingredient.entity.IngredientStatus;
import com.app.expiry_system.ingredient.repository.IngredientRepository;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationGenerateRequest;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationResponse;
import com.app.expiry_system.purchase.dto.PurchaseRunDetailResponse;
import com.app.expiry_system.purchase.dto.PurchaseRunSummaryResponse;
import com.app.expiry_system.purchase.dto.PurchaseSettingRequest;
import com.app.expiry_system.purchase.dto.PurchaseSettingResponse;
import com.app.expiry_system.purchase.entity.PurchaseRecommendation;
import com.app.expiry_system.purchase.entity.PurchaseRecommendationRun;
import com.app.expiry_system.purchase.entity.PurchaseRunSource;
import com.app.expiry_system.purchase.entity.PurchaseRunStatus;
import com.app.expiry_system.purchase.entity.RestaurantPurchaseSetting;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRepository;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRunRepository;
import com.app.expiry_system.purchase.repository.RestaurantPurchaseSettingRepository;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.suggestion.service.KkuAiClient;
import com.app.expiry_system.usage.entity.UsageActionType;
import com.app.expiry_system.usage.entity.UsageHistory;
import com.app.expiry_system.usage.repository.UsageHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchasePlanningService {

    public static final ZoneId PURCHASE_ZONE = ZoneId.of("Asia/Bangkok");
    private static final List<DayOfWeek> DEFAULT_PURCHASE_DAYS = List.of(DayOfWeek.MONDAY);
    private static final int DEFAULT_LOOKBACK_PURCHASE_RUNS = 4;
    private static final LocalTime DEFAULT_NOTIFICATION_TIME = LocalTime.of(0, 1);
    private static final int DEFAULT_SAFETY_BUFFER_PERCENT = 10;
    private static final int RESULT_SCALE = 3;

    private final RestaurantPurchaseSettingRepository settingRepository;
    private final PurchaseRecommendationRepository recommendationRepository;
    private final PurchaseRecommendationRunRepository runRepository;
    private final IngredientRepository ingredientRepository;
    private final UsageHistoryRepository usageHistoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final KkuAiClient kkuAiClient;
    private final PurchaseRunWriter runWriter;
    private final ObjectMapper objectMapper;
    private final int maxHistoryRuns;

    public PurchasePlanningService(RestaurantPurchaseSettingRepository settingRepository,
                                   PurchaseRecommendationRepository recommendationRepository,
                                   PurchaseRecommendationRunRepository runRepository,
                                   IngredientRepository ingredientRepository,
                                   UsageHistoryRepository usageHistoryRepository,
                                   RestaurantRepository restaurantRepository,
                                   KkuAiClient kkuAiClient,
                                   PurchaseRunWriter runWriter,
                                   @Value("${app.purchase.history.max-runs:20}") int maxHistoryRuns) {
        this.settingRepository = settingRepository;
        this.recommendationRepository = recommendationRepository;
        this.runRepository = runRepository;
        this.ingredientRepository = ingredientRepository;
        this.usageHistoryRepository = usageHistoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.kkuAiClient = kkuAiClient;
        this.runWriter = runWriter;
        this.objectMapper = new ObjectMapper();
        this.maxHistoryRuns = maxHistoryRuns;
    }

    public PurchaseSettingResponse getSetting(String restaurantId, AppUser currentUser) {
        validateManagerRestaurantAccess(restaurantId, currentUser);
        return toSettingResponse(findSettingOrDefault(restaurantId));
    }

    @Transactional
    public PurchaseSettingResponse updateSetting(String restaurantId, PurchaseSettingRequest request,
                                                 AppUser currentUser) {
        validateManagerRestaurantAccess(restaurantId, currentUser);

        RestaurantPurchaseSetting setting = settingRepository.findById(restaurantId)
                .orElseGet(RestaurantPurchaseSetting::new);
        setting.setRestaurantId(restaurantId);
        setting.setPurchaseDays(serializePurchaseDays(normalizePurchaseDays(request.getPurchaseDays())));
        setting.setLookbackPurchaseRuns(request.getLookbackPurchaseRuns());
        setting.setNotificationTime(request.getNotificationTime());
        setting.setSafetyBufferPercent(request.getSafetyBufferPercent());

        return toSettingResponse(settingRepository.save(setting));
    }

    /**
     * คืนรายการของรอบล่าสุดที่สำเร็จ เพื่อให้พฤติกรรมเหมือนตอนที่ยังเก็บได้แค่ snapshot เดียว.
     */
    public List<PurchaseRecommendationResponse> getRecommendations(String restaurantId, AppUser currentUser) {
        validateManagerRestaurantAccess(restaurantId, currentUser);
        return runRepository
                .findFirstByRestaurantIdAndStatusOrderByGeneratedAtDesc(restaurantId, PurchaseRunStatus.SUCCESS)
                .map(run -> sortRecommendations(recommendationRepository.findByRunId(run.getId())).stream()
                        .map(this::toRecommendationResponse)
                        .collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    public List<PurchaseRunSummaryResponse> getRuns(String restaurantId, Integer limit, AppUser currentUser) {
        validateManagerRestaurantAccess(restaurantId, currentUser);
        int effectiveLimit = limit != null && limit > 0 ? limit : maxHistoryRuns;
        return runRepository.findByRestaurantIdOrderByGeneratedAtDesc(restaurantId).stream()
                .limit(effectiveLimit)
                .map(this::toRunSummaryResponse)
                .collect(Collectors.toList());
    }

    public PurchaseRunDetailResponse getRunDetail(String runId, AppUser currentUser) {
        PurchaseRecommendationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase recommendation run not found"));
        validateManagerRestaurantAccess(run.getRestaurantId(), currentUser);

        List<PurchaseRecommendationResponse> items =
                sortRecommendations(recommendationRepository.findByRunId(run.getId())).stream()
                        .map(this::toRecommendationResponse)
                        .collect(Collectors.toList());
        return toRunDetailResponse(run, items);
    }

    public List<PurchaseRecommendationResponse> generateRecommendations(PurchaseRecommendationGenerateRequest request,
                                                                        AppUser currentUser) {
        validateManagerRestaurantAccess(request.getRestaurantId(), currentUser);
        return generateForRestaurant(request.getRestaurantId(), LocalDate.now(PURCHASE_ZONE),
                PurchaseRunSource.MANUAL);
    }

    /**
     * ไม่ใช้ {@code @Transactional} ที่ระดับนี้ เพราะขั้นเรียก AI เป็น HTTP call ที่ช้า
     * การเขียนทั้งหมดถูกรวมไว้ใน {@link PurchaseRunWriter} ซึ่งเปิด transaction ของตัวเอง.
     */
    public List<PurchaseRecommendationResponse> generateForRestaurant(String restaurantId, LocalDate runDate,
                                                                      PurchaseRunSource source) {
        RestaurantPurchaseSetting setting = findSettingOrDefault(restaurantId);
        Instant lookbackStart = lookbackStartInstant(setting, runDate);
        List<IngredientGroup> groups = buildIngredientGroups(restaurantId);
        List<UsageHistory> histories = usageHistoryRepository.findByRestaurantId(restaurantId).stream()
                .filter(history -> isConsumptionAction(history.getActionType()))
                .filter(history -> history.getQuantityChanged() != null)
                .filter(history -> !history.getPerformedAt().isBefore(lookbackStart))
                .collect(Collectors.toList());

        PurchaseRecommendationRun run = newRun(restaurantId, runDate, source, setting, lookbackStart);

        List<PurchaseRecommendation> recommendations;
        try {
            recommendations = callAiForRecommendations(setting, runDate, groups, histories);
        } catch (RuntimeException exception) {
            runWriter.saveFailedRun(run, exception.getMessage(), maxHistoryRuns);
            throw exception;
        }

        PurchaseRecommendationRun savedRun = runWriter.saveSuccessfulRun(run, recommendations, maxHistoryRuns);

        return sortRecommendations(recommendationRepository.findByRunId(savedRun.getId())).stream()
                .map(this::toRecommendationResponse)
                .collect(Collectors.toList());
    }

    private PurchaseRecommendationRun newRun(String restaurantId, LocalDate runDate, PurchaseRunSource source,
                                             RestaurantPurchaseSetting setting, Instant lookbackStart) {
        PurchaseRecommendationRun run = new PurchaseRecommendationRun();
        run.setRestaurantId(restaurantId);
        run.setRunDate(runDate);
        run.setSource(source);
        run.setPurchaseDays(setting.getPurchaseDays());
        run.setLookbackPurchaseRuns(setting.getLookbackPurchaseRuns());
        run.setSafetyBufferPercent(setting.getSafetyBufferPercent());
        run.setLookbackStartAt(lookbackStart);
        return run;
    }

    public boolean isPurchaseDay(RestaurantPurchaseSetting setting, LocalDate date) {
        return parsePurchaseDays(setting.getPurchaseDays()).contains(date.getDayOfWeek());
    }

    private List<PurchaseRecommendation> callAiForRecommendations(RestaurantPurchaseSetting setting, LocalDate runDate,
                                                                  List<IngredientGroup> groups,
                                                                  List<UsageHistory> histories) {
        String systemPrompt = """
                You are a restaurant purchase planning assistant.
                Analyze inventory and usage history, then recommend what ingredients to buy today.
                All user-facing text must be in Thai.
                Return only valid JSON. Do not include markdown, comments, or extra text.
                The JSON schema must be:
                {
                  "recommendations": [
                    {
                      "ingredientName": "string",
                      "category": "string",
                      "unit": "string",
                      "currentQuantity": 0,
                      "averageDailyUsage": 0,
                      "estimatedConsumptionUntilNextCycle": 0,
                      "recommendedBuyQuantity": 0,
                      "reason": "Thai string, short and practical",
                      "confidence": "HIGH|MEDIUM|LOW"
                    }
                  ]
                }
                """;

        String userPrompt = """
                Generate purchase recommendations for restaurantId=%s on %s.
                Purchase days: %s.
                Lookback purchase runs: %d.
                Safety buffer percent: %d.
                Current stock groups:
                %s
                Usage history in lookback window:
                %s
                Rules:
                - recommendedBuyQuantity must use the same unit as the stock group.
                - Use current stock, usage trend, purchase schedule, and safety buffer.
                - Include items with recommendedBuyQuantity 0 when current stock is enough.
                - confidence should be LOW when usage history is insufficient.
                - reason must be Thai, concise, and no longer than 120 characters.
                - reason must explain why to buy or not buy, using current stock and usage history.
                - If there is no recent usage, say "ยังไม่มีประวัติการใช้ล่าสุด" and avoid saying the stock is definitely enough.
                - Do not use English in menu text, reason, or explanations.
                - Return at least one recommendation when stock groups are provided.
                """.formatted(
                setting.getRestaurantId(),
                runDate,
                setting.getPurchaseDays(),
                setting.getLookbackPurchaseRuns(),
                setting.getSafetyBufferPercent(),
                formatGroups(groups),
                formatHistories(histories)
        );

        String content = kkuAiClient.completeJson(systemPrompt, userPrompt);
        return parseAiRecommendations(setting.getRestaurantId(), content);
    }

    private List<PurchaseRecommendation> parseAiRecommendations(String restaurantId, String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode recommendationsNode = root.isArray() ? root : root.path("recommendations");
            if (!recommendationsNode.isArray() || recommendationsNode.isEmpty()) {
                throw new IllegalArgumentException("KKU AI response did not contain recommendations");
            }

            List<PurchaseRecommendation> recommendations = new ArrayList<>();
            for (JsonNode node : recommendationsNode) {
                PurchaseRecommendation recommendation = new PurchaseRecommendation();
                recommendation.setRestaurantId(restaurantId);
                recommendation.setIngredientName(requiredText(node, "ingredientName"));
                recommendation.setCategory(requiredText(node, "category"));
                recommendation.setUnit(requiredText(node, "unit"));
                recommendation.setCurrentQuantity(number(node, "currentQuantity"));
                recommendation.setAverageDailyUsage(number(node, "averageDailyUsage"));
                recommendation.setEstimatedConsumptionUntilNextCycle(number(node, "estimatedConsumptionUntilNextCycle"));
                recommendation.setRecommendedBuyQuantity(number(node, "recommendedBuyQuantity"));
                recommendation.setReason(requiredText(node, "reason"));
                recommendation.setConfidence(normalizeConfidence(requiredText(node, "confidence")));
                recommendations.add(recommendation);
            }
            return recommendations;
        } catch (Exception exception) {
            throw new IllegalArgumentException("KKU AI purchase recommendation parse failed: " + exception.getMessage());
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText().trim();
    }

    private BigDecimal number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) {
            throw new IllegalArgumentException(field + " must be a number");
        }
        BigDecimal number = value.decimalValue();
        if (number.compareTo(BigDecimal.ZERO) < 0) {
            number = BigDecimal.ZERO;
        }
        return number.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeConfidence(String confidence) {
        String normalized = confidence.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("HIGH", "MEDIUM", "LOW").contains(normalized)) {
            return "LOW";
        }
        return normalized;
    }

    private Map<IngredientGroupKey, IngredientGroup> buildIngredientGroupMap(String restaurantId) {
        Map<IngredientGroupKey, IngredientGroup> groups = new LinkedHashMap<>();
        ingredientRepository.findByRestaurantId(restaurantId).stream()
                .filter(ingredient -> ingredient.getStatus() != IngredientStatus.DELETED)
                .sorted(Comparator.comparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Ingredient::getCategory, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Ingredient::getUnit, String.CASE_INSENSITIVE_ORDER))
                .forEach(ingredient -> {
                    IngredientGroupKey key = IngredientGroupKey.from(ingredient);
                    IngredientGroup group = groups.computeIfAbsent(key, ignored -> new IngredientGroup(key, ingredient));
                    group.ingredientIds.add(ingredient.getId());
                    if (ingredient.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        group.currentQuantity = group.currentQuantity.add(ingredient.getQuantity());
                    }
                });
        return groups;
    }

    private List<IngredientGroup> buildIngredientGroups(String restaurantId) {
        return new ArrayList<>(buildIngredientGroupMap(restaurantId).values());
    }

    private Instant lookbackStartInstant(RestaurantPurchaseSetting setting, LocalDate runDate) {
        List<DayOfWeek> purchaseDays = parsePurchaseDays(setting.getPurchaseDays());
        LocalDate cursor = runDate.minusDays(1);
        int foundRuns = 0;
        while (foundRuns < setting.getLookbackPurchaseRuns()) {
            if (purchaseDays.contains(cursor.getDayOfWeek())) {
                foundRuns++;
            }
            if (foundRuns < setting.getLookbackPurchaseRuns()) {
                cursor = cursor.minusDays(1);
            }
        }
        return cursor.atStartOfDay(PURCHASE_ZONE).toInstant();
    }

    private String formatGroups(List<IngredientGroup> groups) {
        if (groups.isEmpty()) {
            return "- none";
        }
        return groups.stream()
                .map(group -> "- %s | category=%s | quantity=%s %s | ingredientIds=%s"
                        .formatted(group.ingredientName, group.category, group.currentQuantity.stripTrailingZeros().toPlainString(),
                                group.unit, String.join(",", group.ingredientIds)))
                .collect(Collectors.joining("\n"));
    }

    private String formatHistories(List<UsageHistory> histories) {
        if (histories.isEmpty()) {
            return "- none";
        }
        return histories.stream()
                .sorted(Comparator.comparing(UsageHistory::getPerformedAt).reversed())
                .map(history -> "- %s | %s | %s %s | ingredient=%s"
                        .formatted(history.getPerformedAt(), history.getActionType(),
                                history.getQuantityChanged().stripTrailingZeros().toPlainString(),
                                history.getUnit(), history.getIngredientName()))
                .collect(Collectors.joining("\n"));
    }

    public RestaurantPurchaseSetting findSettingOrDefault(String restaurantId) {
        return settingRepository.findById(restaurantId).orElseGet(() -> defaultSetting(restaurantId));
    }

    private RestaurantPurchaseSetting defaultSetting(String restaurantId) {
        RestaurantPurchaseSetting setting = new RestaurantPurchaseSetting();
        setting.setRestaurantId(restaurantId);
        setting.setPurchaseDays(serializePurchaseDays(DEFAULT_PURCHASE_DAYS));
        setting.setLookbackPurchaseRuns(DEFAULT_LOOKBACK_PURCHASE_RUNS);
        setting.setNotificationTime(DEFAULT_NOTIFICATION_TIME);
        setting.setSafetyBufferPercent(DEFAULT_SAFETY_BUFFER_PERCENT);
        return setting;
    }

    private List<PurchaseRecommendation> sortRecommendations(List<PurchaseRecommendation> recommendations) {
        return recommendations.stream()
                .sorted(Comparator.comparing(PurchaseRecommendation::getRecommendedBuyQuantity).reversed()
                        .thenComparing(PurchaseRecommendation::getIngredientName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private boolean isConsumptionAction(UsageActionType actionType) {
        return actionType == UsageActionType.CONSUMED || actionType == UsageActionType.USED;
    }

    private List<DayOfWeek> normalizePurchaseDays(List<DayOfWeek> purchaseDays) {
        return purchaseDays.stream()
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .collect(Collectors.toList());
    }

    private String serializePurchaseDays(List<DayOfWeek> purchaseDays) {
        return normalizePurchaseDays(purchaseDays).stream()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }

    private List<DayOfWeek> parsePurchaseDays(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PURCHASE_DAYS;
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toList());
    }

    private void validateManagerRestaurantAccess(String restaurantId, AppUser currentUser) {
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("restaurantId is required");
        }
        if (currentUser.getRestaurantId() == null || currentUser.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("User is not assigned to any restaurant");
        }
        if (!restaurantId.equals(currentUser.getRestaurantId())) {
            throw new IllegalArgumentException("Unauthorized to access this restaurant");
        }
        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only Managers can access purchase planning");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException("Restaurant not found");
        }
    }

    private PurchaseSettingResponse toSettingResponse(RestaurantPurchaseSetting setting) {
        return new PurchaseSettingResponse(
                setting.getRestaurantId(),
                parsePurchaseDays(setting.getPurchaseDays()),
                setting.getLookbackPurchaseRuns(),
                setting.getNotificationTime(),
                setting.getSafetyBufferPercent(),
                setting.getUpdatedAt()
        );
    }

    private PurchaseRecommendationResponse toRecommendationResponse(PurchaseRecommendation recommendation) {
        return new PurchaseRecommendationResponse(
                recommendation.getId(),
                recommendation.getRestaurantId(),
                recommendation.getRunId(),
                recommendation.getIngredientName(),
                recommendation.getCategory(),
                recommendation.getUnit(),
                recommendation.getCurrentQuantity(),
                recommendation.getAverageDailyUsage(),
                recommendation.getEstimatedConsumptionUntilNextCycle(),
                recommendation.getRecommendedBuyQuantity(),
                recommendation.getReason(),
                recommendation.getConfidence(),
                recommendation.getGeneratedAt()
        );
    }

    private PurchaseRunSummaryResponse toRunSummaryResponse(PurchaseRecommendationRun run) {
        return new PurchaseRunSummaryResponse(
                run.getId(),
                run.getRestaurantId(),
                run.getRunDate(),
                run.getGeneratedAt(),
                run.getSource().name(),
                run.getStatus().name(),
                run.getErrorMessage(),
                run.getItemCount(),
                run.getTotalBuyItems(),
                parsePurchaseDays(run.getPurchaseDays()),
                run.getLookbackPurchaseRuns(),
                run.getSafetyBufferPercent(),
                run.getLookbackStartAt()
        );
    }

    private PurchaseRunDetailResponse toRunDetailResponse(PurchaseRecommendationRun run,
                                                          List<PurchaseRecommendationResponse> items) {
        return new PurchaseRunDetailResponse(
                run.getId(),
                run.getRestaurantId(),
                run.getRunDate(),
                run.getGeneratedAt(),
                run.getSource().name(),
                run.getStatus().name(),
                run.getErrorMessage(),
                run.getItemCount(),
                run.getTotalBuyItems(),
                parsePurchaseDays(run.getPurchaseDays()),
                run.getLookbackPurchaseRuns(),
                run.getSafetyBufferPercent(),
                run.getLookbackStartAt(),
                items
        );
    }

    private static class IngredientGroupKey {
        private final String restaurantId;
        private final String normalizedName;
        private final String category;
        private final String unit;

        private IngredientGroupKey(String restaurantId, String normalizedName, String category, String unit) {
            this.restaurantId = restaurantId;
            this.normalizedName = normalizedName;
            this.category = category;
            this.unit = unit;
        }

        private static IngredientGroupKey from(Ingredient ingredient) {
            return new IngredientGroupKey(
                    ingredient.getRestaurantId(),
                    normalize(ingredient.getName()),
                    normalize(ingredient.getCategory()),
                    normalize(ingredient.getUnit())
            );
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof IngredientGroupKey that)) {
                return false;
            }
            return Objects.equals(restaurantId, that.restaurantId)
                    && Objects.equals(normalizedName, that.normalizedName)
                    && Objects.equals(category, that.category)
                    && Objects.equals(unit, that.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(restaurantId, normalizedName, category, unit);
        }
    }

    private static class IngredientGroup {
        private final IngredientGroupKey key;
        private final String ingredientName;
        private final String category;
        private final String unit;
        private final List<String> ingredientIds = new ArrayList<>();
        private BigDecimal currentQuantity = BigDecimal.ZERO;

        private IngredientGroup(IngredientGroupKey key, Ingredient representative) {
            this.key = key;
            this.ingredientName = representative.getName();
            this.category = representative.getCategory();
            this.unit = representative.getUnit();
        }
    }
}
