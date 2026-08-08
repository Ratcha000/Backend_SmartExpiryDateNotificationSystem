package com.app.expiry_system.usage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.ingredient.dto.IngredientRequest;
import com.app.expiry_system.ingredient.service.IngredientService;
import com.app.expiry_system.restaurant.entity.Restaurant;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import com.app.expiry_system.usage.dto.UsageHistoryRequest;
import com.app.expiry_system.usage.entity.UsageActionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UsageHistoryServiceIntegrationTest {

    @Autowired
    private UsageHistoryService usageHistoryService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private AppUser manager;
    private AppUser employee;
    private Restaurant restaurant;
    private String ingredientId;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setName("History Test Kitchen");
        restaurant.setManagerId("manager-seed");
        restaurant = restaurantRepository.save(restaurant);

        manager = new AppUser();
        manager.setEmail("history-manager@test.com");
        manager.setPasswordHash("hash");
        manager.setDisplayName("Manager");
        manager.setRole(UserRole.MANAGER);
        manager.setRestaurantId(restaurant.getId());
        manager = appUserRepository.save(manager);

        employee = new AppUser();
        employee.setEmail("history-employee@test.com");
        employee.setPasswordHash("hash");
        employee.setDisplayName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setRestaurantId(restaurant.getId());
        employee = appUserRepository.save(employee);

        restaurant.setManagerId(manager.getId());
        restaurantRepository.save(restaurant);

        IngredientRequest ingredientRequest = new IngredientRequest();
        ingredientRequest.setRestaurantId(restaurant.getId());
        ingredientRequest.setName("Palm sugar");
        ingredientRequest.setCategory("dry");
        ingredientRequest.setInitialQuantity(new BigDecimal("5.0"));
        ingredientRequest.setQuantity(new BigDecimal("5.0"));
        ingredientRequest.setUnit("kg");
        ingredientRequest.setExpiryDate(LocalDate.now().plusDays(30));
        ingredientRequest.setNotifyDaysBefore(5);

        ingredientId = ingredientService.createIngredient(ingredientRequest, employee).getId();
    }

    @Test
    void createAndQueryHistory_shouldWork() {
        UsageHistoryRequest request = new UsageHistoryRequest();
        request.setIngredientId(ingredientId);
        request.setActionType(UsageActionType.ADJUSTED);
        request.setQuantityBefore(new BigDecimal("5.0"));
        request.setQuantityAfter(new BigDecimal("4.8"));
        request.setQuantityChanged(new BigDecimal("0.2"));
        request.setNote("Manual audit");

        var created = usageHistoryService.createHistory(request, employee);
        assertEquals(ingredientId, created.getIngredientId());
        assertEquals(UsageActionType.ADJUSTED, created.getActionType());

        var histories = usageHistoryService.getHistories(restaurant.getId(), ingredientId, "ADJUSTED", manager);
        assertEquals(1, histories.stream().filter(item -> item.getId().equals(created.getId())).count());
    }

    @Test
    void employeeCannotReadHistoryReports() {
        assertThrows(IllegalArgumentException.class,
                () -> usageHistoryService.getHistories(restaurant.getId(), null, null, employee));
    }
}
