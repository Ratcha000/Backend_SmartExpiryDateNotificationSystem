package com.app.expiry_system.purchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.ingredient.dto.IngredientRequest;
import com.app.expiry_system.ingredient.repository.IngredientRepository;
import com.app.expiry_system.ingredient.service.IngredientService;
import com.app.expiry_system.notification.entity.NotificationType;
import com.app.expiry_system.notification.repository.AppNotificationRepository;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationGenerateRequest;
import com.app.expiry_system.purchase.dto.PurchaseRecommendationResponse;
import com.app.expiry_system.purchase.dto.PurchaseRunSummaryResponse;
import com.app.expiry_system.purchase.dto.PurchaseSettingRequest;
import com.app.expiry_system.purchase.entity.PurchaseRecommendationRun;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRepository;
import com.app.expiry_system.purchase.repository.PurchaseRecommendationRunRepository;
import com.app.expiry_system.restaurant.entity.Restaurant;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.suggestion.service.KkuAiClient;
import com.app.expiry_system.usage.entity.UsageActionType;
import com.app.expiry_system.usage.entity.UsageHistory;
import com.app.expiry_system.usage.repository.UsageHistoryRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PurchasePlanningServiceIntegrationTest {

    @Autowired
    private PurchasePlanningService purchasePlanningService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UsageHistoryRepository usageHistoryRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private PurchasePlanningScheduler purchasePlanningScheduler;

    @Autowired
    private AppNotificationRepository notificationRepository;

    @Autowired
    private PurchaseRecommendationRunRepository runRepository;

    @Autowired
    private PurchaseRecommendationRepository recommendationRepository;

    @Autowired
    private PurchaseRunWriter purchaseRunWriter;

    private AppUser manager;
    private AppUser employee;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setName("Purchase Test Kitchen");
        restaurant.setManagerId("manager-seed");
        restaurant = restaurantRepository.save(restaurant);

        manager = new AppUser();
        manager.setEmail("purchase-manager@test.com");
        manager.setPasswordHash("hash");
        manager.setDisplayName("Manager");
        manager.setRole(UserRole.MANAGER);
        manager.setRestaurantId(restaurant.getId());
        manager = appUserRepository.save(manager);

        employee = new AppUser();
        employee.setEmail("purchase-employee@test.com");
        employee.setPasswordHash("hash");
        employee.setDisplayName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setRestaurantId(restaurant.getId());
        employee = appUserRepository.save(employee);

        restaurant.setManagerId(manager.getId());
        restaurantRepository.save(restaurant);
    }

    @Test
    void getSetting_shouldReturnDefaultWhenSettingDoesNotExist() {
        var setting = purchasePlanningService.getSetting(restaurant.getId(), manager);

        assertEquals(restaurant.getId(), setting.getRestaurantId());
        assertEquals(List.of(DayOfWeek.MONDAY), setting.getPurchaseDays());
        assertEquals(4, setting.getLookbackPurchaseRuns());
        assertEquals(LocalTime.of(0, 1), setting.getNotificationTime());
        assertEquals(10, setting.getSafetyBufferPercent());
    }

    @Test
    void updateSetting_shouldPersistManagerSetting() {
        PurchaseSettingRequest request = new PurchaseSettingRequest();
        request.setPurchaseDays(List.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));
        request.setLookbackPurchaseRuns(6);
        request.setNotificationTime(LocalTime.of(0, 1));
        request.setSafetyBufferPercent(20);

        var updated = purchasePlanningService.updateSetting(restaurant.getId(), request, manager);

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), updated.getPurchaseDays());
        assertEquals(6, updated.getLookbackPurchaseRuns());
        assertEquals(LocalTime.of(0, 1), updated.getNotificationTime());
        assertEquals(20, updated.getSafetyBufferPercent());
    }

    @Test
    void employeeCannotAccessPurchasePlanning() {
        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.getSetting(restaurant.getId(), employee));
    }

    @Test
    void generateRecommendations_shouldSaveAiRecommendations() {
        updateSetting(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), 4, 10);
        mockAiRecommendations("""
                {
                  "recommendations": [
                    {
                      "ingredientName": "Chicken Breast",
                      "category": "meat",
                      "unit": "kg",
                      "currentQuantity": 3,
                      "averageDailyUsage": 1.2,
                      "estimatedConsumptionUntilNextCycle": 4.8,
                      "recommendedBuyQuantity": 3,
                      "reason": "AI recommends buying enough for the next purchase window.",
                      "confidence": "HIGH"
                    }
                  ]
                }
                """);

        String firstLotId = createIngredient("Chicken Breast", "meat", "kg", "2.0");
        String secondLotId = createIngredient("Chicken Breast", "meat", "kg", "1.0");
        addUsage(firstLotId, UsageActionType.CONSUMED, "6.0", Instant.now().minus(2, ChronoUnit.DAYS));
        addUsage(secondLotId, UsageActionType.USED, "4.0", Instant.now().minus(3, ChronoUnit.DAYS));

        var recommendations = purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);

        assertEquals(1, recommendations.size());
        PurchaseRecommendationResponse recommendation = recommendations.get(0);
        assertEquals("Chicken Breast", recommendation.getIngredientName());
        assertEquals("meat", recommendation.getCategory());
        assertEquals("kg", recommendation.getUnit());
        assertEquals(0, recommendation.getCurrentQuantity().compareTo(new BigDecimal("3.000")));
        assertEquals(0, recommendation.getAverageDailyUsage().compareTo(new BigDecimal("1.200")));
        assertEquals(0, recommendation.getEstimatedConsumptionUntilNextCycle().compareTo(new BigDecimal("4.800")));
        assertEquals(0, recommendation.getRecommendedBuyQuantity().compareTo(new BigDecimal("3.000")));
        assertEquals("HIGH", recommendation.getConfidence());

        List<PurchaseRecommendationResponse> saved = purchasePlanningService.getRecommendations(restaurant.getId(), manager);
        assertEquals(1, saved.size());
        assertEquals(0, saved.get(0).getRecommendedBuyQuantity().compareTo(new BigDecimal("3.000")));
    }

    @Test
    void generateRecommendations_shouldNormalizeInvalidAiConfidenceToLow() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 0);
        mockAiRecommendations("""
                {
                  "recommendations": [
                    {
                      "ingredientName": "Rice",
                      "category": "dry",
                      "unit": "kg",
                      "currentQuantity": 20,
                      "averageDailyUsage": 1,
                      "estimatedConsumptionUntilNextCycle": 2,
                      "recommendedBuyQuantity": 0,
                      "reason": "Stock is enough.",
                      "confidence": "UNKNOWN"
                    }
                  ]
                }
                """);

        String ingredientId = createIngredient("Rice", "dry", "kg", "20.0");
        addUsage(ingredientId, UsageActionType.CONSUMED, "5.0", Instant.now().minus(1, ChronoUnit.DAYS));

        var recommendations = purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);

        assertEquals(1, recommendations.size());
        assertEquals(0, recommendations.get(0).getRecommendedBuyQuantity().compareTo(new BigDecimal("0.000")));
        assertEquals("LOW", recommendations.get(0).getConfidence());
    }

    @Test
    void generateRecommendations_shouldFailWhenAiResponseIsInvalid() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 0);
        mockAiRecommendations("{\"items\": []}");

        createIngredient("Egg", "dairy", "pcs", "1.0");

        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager));
    }

    @Test
    void scheduler_shouldGenerateRecommendationsAndNotifyManagerOnPurchaseDay() {
        updateSetting(List.of(LocalDate.now(PurchasePlanningService.PURCHASE_ZONE).getDayOfWeek()), 4, 10,
                LocalTime.now(PurchasePlanningService.PURCHASE_ZONE).withSecond(0).withNano(0));
        mockAiRecommendations("""
                {
                  "recommendations": [
                    {
                      "ingredientName": "Corn",
                      "category": "vegetable",
                      "unit": "kg",
                      "currentQuantity": 2,
                      "averageDailyUsage": 0.5,
                      "estimatedConsumptionUntilNextCycle": 1,
                      "recommendedBuyQuantity": 1,
                      "reason": "Buy for today's prep.",
                      "confidence": "MEDIUM"
                    }
                  ]
                }
                """);
        createIngredient("Corn", "vegetable", "kg", "2.0");

        purchasePlanningScheduler.runScheduledPurchasePlanning();

        var notifications = notificationRepository.findByUserId(manager.getId());
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.PURCHASE_RECOMMENDATION, notifications.get(0).getType());
    }

    @Test
    void generateRecommendations_shouldKeepEveryRoundInHistory() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Chicken Breast", "meat", "kg", "2.0");

        mockAiRecommendations(aiResponse("Chicken Breast", "2", "HIGH"));
        var firstRound = purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);
        String firstRunId = firstRound.get(0).getRunId();
        spaceOutGeneratedAt(firstRunId, Instant.now().minusSeconds(60));

        mockAiRecommendations(aiResponse("Chicken Breast", "5", "MEDIUM"));
        var secondRound = purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);
        String secondRunId = secondRound.get(0).getRunId();

        List<PurchaseRunSummaryResponse> runs = purchasePlanningService.getRuns(restaurant.getId(), null, manager);
        assertEquals(2, runs.size());
        assertEquals(secondRunId, runs.get(0).getRunId());
        assertEquals(firstRunId, runs.get(1).getRunId());
        assertEquals("MANUAL", runs.get(0).getSource());
        assertEquals("SUCCESS", runs.get(0).getStatus());
        assertEquals(1, runs.get(0).getItemCount());
        assertEquals(1, runs.get(0).getTotalBuyItems());
        assertEquals(List.of(DayOfWeek.MONDAY), runs.get(0).getPurchaseDays());
        assertEquals(4, runs.get(0).getLookbackPurchaseRuns());
        assertEquals(10, runs.get(0).getSafetyBufferPercent());
        assertNotNull(runs.get(0).getLookbackStartAt());

        // รอบเก่ายังคืนรายการของตัวเองได้ครบ ไม่ถูกรอบใหม่ลบทิ้ง
        var firstDetail = purchasePlanningService.getRunDetail(firstRunId, manager);
        assertEquals(1, firstDetail.getItems().size());
        assertEquals(0, firstDetail.getItems().get(0).getRecommendedBuyQuantity().compareTo(new BigDecimal("2.000")));

        // เส้นเดิมยังคืนเฉพาะรอบล่าสุดเหมือนก่อนหน้านี้
        var latest = purchasePlanningService.getRecommendations(restaurant.getId(), manager);
        assertEquals(1, latest.size());
        assertEquals(secondRunId, latest.get(0).getRunId());
        assertEquals(0, latest.get(0).getRecommendedBuyQuantity().compareTo(new BigDecimal("5.000")));
    }

    @Test
    void generateRecommendations_shouldRecordFailedRunAndKeepPreviousRound() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Rice", "dry", "kg", "10.0");

        mockAiRecommendations(aiResponse("Rice", "3", "HIGH"));
        var successRound = purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);
        String successRunId = successRound.get(0).getRunId();
        spaceOutGeneratedAt(successRunId, Instant.now().minusSeconds(60));

        mockAiRecommendations("{\"items\": []}");
        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager));

        List<PurchaseRunSummaryResponse> runs = purchasePlanningService.getRuns(restaurant.getId(), null, manager);
        assertEquals(2, runs.size());
        assertEquals("FAILED", runs.get(0).getStatus());
        assertNotNull(runs.get(0).getErrorMessage());
        assertEquals(0, runs.get(0).getItemCount());
        assertEquals("SUCCESS", runs.get(1).getStatus());

        // รอบที่สำเร็จก่อนหน้ายังเป็นผลลัพธ์ล่าสุดที่ frontend เห็น
        var latest = purchasePlanningService.getRecommendations(restaurant.getId(), manager);
        assertEquals(1, latest.size());
        assertEquals(successRunId, latest.get(0).getRunId());
    }

    @Test
    void pruneOldRuns_shouldDropOldestRunsAndTheirItems() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Corn", "vegetable", "kg", "4.0");

        mockAiRecommendations(aiResponse("Corn", "1", "LOW"));
        String oldestRunId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();
        spaceOutGeneratedAt(oldestRunId, Instant.now().minusSeconds(120));

        String middleRunId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();
        spaceOutGeneratedAt(middleRunId, Instant.now().minusSeconds(60));

        String newestRunId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();

        purchaseRunWriter.pruneOldRuns(restaurant.getId(), 2);

        List<PurchaseRunSummaryResponse> runs = purchasePlanningService.getRuns(restaurant.getId(), null, manager);
        assertEquals(2, runs.size());
        assertEquals(newestRunId, runs.get(0).getRunId());
        assertEquals(middleRunId, runs.get(1).getRunId());
        // รายการของรอบที่ถูกตัดต้องหายไปด้วย ไม่เหลือแถวกำพร้า
        assertTrue(recommendationRepository.findByRunId(oldestRunId).isEmpty());
    }

    @Test
    void getRuns_shouldRespectLimit() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Egg", "dairy", "pcs", "12.0");

        mockAiRecommendations(aiResponse("Egg", "6", "MEDIUM"));
        String firstRunId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();
        spaceOutGeneratedAt(firstRunId, Instant.now().minusSeconds(60));
        purchasePlanningService.generateRecommendations(generateRequest(restaurant.getId()), manager);

        assertEquals(1, purchasePlanningService.getRuns(restaurant.getId(), 1, manager).size());
        assertEquals(2, purchasePlanningService.getRuns(restaurant.getId(), null, manager).size());
    }

    @Test
    void employeeCannotAccessRunHistory() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Milk", "dairy", "l", "3.0");
        mockAiRecommendations(aiResponse("Milk", "2", "HIGH"));
        String runId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();

        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.getRuns(restaurant.getId(), null, employee));
        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.getRunDetail(runId, employee));
    }

    @Test
    void getRunDetail_shouldRejectRunFromAnotherRestaurant() {
        updateSetting(List.of(DayOfWeek.MONDAY), 4, 10);
        createIngredient("Beef", "meat", "kg", "5.0");
        mockAiRecommendations(aiResponse("Beef", "2", "HIGH"));
        String runId = purchasePlanningService
                .generateRecommendations(generateRequest(restaurant.getId()), manager).get(0).getRunId();

        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setName("Other Kitchen");
        otherRestaurant.setManagerId("other-seed");
        otherRestaurant = restaurantRepository.save(otherRestaurant);

        AppUser otherManager = new AppUser();
        otherManager.setEmail("other-manager@test.com");
        otherManager.setPasswordHash("hash");
        otherManager.setDisplayName("Other Manager");
        otherManager.setRole(UserRole.MANAGER);
        otherManager.setRestaurantId(otherRestaurant.getId());
        otherManager = appUserRepository.save(otherManager);

        AppUser caller = otherManager;
        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.getRunDetail(runId, caller));
    }

    @Test
    void getRunDetail_shouldRejectUnknownRunId() {
        assertThrows(IllegalArgumentException.class,
                () -> purchasePlanningService.getRunDetail("does-not-exist", manager));
    }

    @Test
    void scheduler_shouldRecordRunAsScheduled() {
        updateSetting(List.of(LocalDate.now(PurchasePlanningService.PURCHASE_ZONE).getDayOfWeek()), 4, 10,
                LocalTime.now(PurchasePlanningService.PURCHASE_ZONE).withSecond(0).withNano(0));
        mockAiRecommendations(aiResponse("Onion", "3", "MEDIUM"));
        createIngredient("Onion", "vegetable", "kg", "1.0");

        purchasePlanningScheduler.runScheduledPurchasePlanning();

        List<PurchaseRunSummaryResponse> runs = purchasePlanningService.getRuns(restaurant.getId(), null, manager);
        assertEquals(1, runs.size());
        assertEquals("SCHEDULED", runs.get(0).getSource());
        assertEquals("SUCCESS", runs.get(0).getStatus());
    }

    /**
     * รอบที่ถูกสร้างติด ๆ กันในเทสต์อาจมี generatedAt ชนกันจนลำดับไม่แน่นอน
     * จึงถอยเวลาของรอบเก่าให้ห่างออกไปเพื่อให้การเรียงลำดับเป็นค่าที่คาดเดาได้.
     */
    private void spaceOutGeneratedAt(String runId, Instant generatedAt) {
        PurchaseRecommendationRun run = runRepository.findById(runId).orElseThrow();
        run.setGeneratedAt(generatedAt);
        runRepository.save(run);
    }

    private String aiResponse(String ingredientName, String recommendedBuyQuantity, String confidence) {
        return """
                {
                  "recommendations": [
                    {
                      "ingredientName": "%s",
                      "category": "test",
                      "unit": "kg",
                      "currentQuantity": 1,
                      "averageDailyUsage": 1,
                      "estimatedConsumptionUntilNextCycle": 2,
                      "recommendedBuyQuantity": %s,
                      "reason": "test reason",
                      "confidence": "%s"
                    }
                  ]
                }
                """.formatted(ingredientName, recommendedBuyQuantity, confidence);
    }

    private void updateSetting(List<DayOfWeek> purchaseDays, int lookbackPurchaseRuns, int safetyBufferPercent) {
        updateSetting(purchaseDays, lookbackPurchaseRuns, safetyBufferPercent, LocalTime.of(0, 1));
    }

    private void updateSetting(List<DayOfWeek> purchaseDays, int lookbackPurchaseRuns, int safetyBufferPercent,
                               LocalTime notificationTime) {
        PurchaseSettingRequest setting = new PurchaseSettingRequest();
        setting.setPurchaseDays(purchaseDays);
        setting.setLookbackPurchaseRuns(lookbackPurchaseRuns);
        setting.setNotificationTime(notificationTime);
        setting.setSafetyBufferPercent(safetyBufferPercent);
        purchasePlanningService.updateSetting(restaurant.getId(), setting, manager);
    }

    private PurchaseRecommendationGenerateRequest generateRequest(String restaurantId) {
        PurchaseRecommendationGenerateRequest request = new PurchaseRecommendationGenerateRequest();
        request.setRestaurantId(restaurantId);
        return request;
    }

    private void mockAiRecommendations(String response) {
        FakeKkuAiClient.response = response;
    }

    private String createIngredient(String name, String category, String unit, String quantity) {
        IngredientRequest request = new IngredientRequest();
        request.setRestaurantId(restaurant.getId());
        request.setName(name);
        request.setCategory(category);
        request.setInitialQuantity(new BigDecimal(quantity));
        request.setQuantity(new BigDecimal(quantity));
        request.setUnit(unit);
        request.setExpiryDate(LocalDate.now().plusDays(30));
        request.setNotifyDaysBefore(5);
        return ingredientService.createIngredient(request, employee).getId();
    }

    private void addUsage(String ingredientId, UsageActionType actionType, String quantityChanged, Instant performedAt) {
        var ingredient = ingredientRepository.findById(ingredientId).orElseThrow();
        UsageHistory history = new UsageHistory();
        history.setIngredientId(ingredient.getId());
        history.setIngredientName(ingredient.getName());
        history.setActionType(actionType);
        history.setQuantityChanged(new BigDecimal(quantityChanged));
        history.setUnit(ingredient.getUnit());
        history.setQuantityBefore(ingredient.getQuantity().add(new BigDecimal(quantityChanged)));
        history.setQuantityAfter(ingredient.getQuantity());
        history.setPerformedBy(employee.getId());
        history.setPerformedAt(performedAt);
        history.setRestaurantId(restaurant.getId());
        usageHistoryRepository.save(history);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        KkuAiClient kkuAiClient() {
            return new FakeKkuAiClient();
        }
    }

    static class FakeKkuAiClient extends KkuAiClient {

        private static String response = """
                {
                  "recommendations": [
                    {
                      "ingredientName": "Default",
                      "category": "default",
                      "unit": "kg",
                      "currentQuantity": 0,
                      "averageDailyUsage": 0,
                      "estimatedConsumptionUntilNextCycle": 0,
                      "recommendedBuyQuantity": 0,
                      "reason": "default",
                      "confidence": "LOW"
                    }
                  ]
                }
                """;

        FakeKkuAiClient() {
            super("http://localhost", "test-api-key", "test-model");
        }

        @Override
        public String completeJson(String systemPrompt, String userPrompt) {
            return response;
        }
    }
}
