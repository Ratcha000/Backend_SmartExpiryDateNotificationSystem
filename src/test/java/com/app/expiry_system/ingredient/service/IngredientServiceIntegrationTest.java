package com.app.expiry_system.ingredient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.ingredient.dto.IngredientBatchItemRequest;
import com.app.expiry_system.ingredient.dto.IngredientBatchRequest;
import com.app.expiry_system.ingredient.dto.IngredientRequest;
import com.app.expiry_system.ingredient.dto.QuantityAdjustmentRequest;
import com.app.expiry_system.ingredient.dto.QuantityChangeRequest;
import com.app.expiry_system.ingredient.entity.IngredientStatus;
import com.app.expiry_system.restaurant.entity.Restaurant;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class IngredientServiceIntegrationTest {

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private AppUser manager;
    private AppUser employee;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setName("Test Kitchen");
        restaurant.setManagerId("manager-seed");
        restaurant = restaurantRepository.save(restaurant);

        manager = new AppUser();
        manager.setEmail("manager@test.com");
        manager.setPasswordHash("hash");
        manager.setDisplayName("Manager");
        manager.setRole(UserRole.MANAGER);
        manager.setRestaurantId(restaurant.getId());
        manager = appUserRepository.save(manager);

        employee = new AppUser();
        employee.setEmail("employee@test.com");
        employee.setPasswordHash("hash");
        employee.setDisplayName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setRestaurantId(restaurant.getId());
        employee = appUserRepository.save(employee);

        restaurant.setManagerId(manager.getId());
        restaurantRepository.save(restaurant);
    }

    @Test
    void createAndConsumeIngredient_shouldUpdateQuantity() {
        IngredientRequest request = new IngredientRequest();
        request.setRestaurantId(restaurant.getId());
        request.setName("Pork loin");
        request.setCategory("meat");
        request.setInitialQuantity(new BigDecimal("12.0"));
        request.setQuantity(new BigDecimal("12.0"));
        request.setUnit("kg");
        request.setExpiryDate(LocalDate.now().plusDays(5));
        request.setNotifyDaysBefore(2);

        var created = ingredientService.createIngredient(request, employee);
        assertEquals(new BigDecimal("12"), created.getQuantity());
        assertEquals(IngredientStatus.ACTIVE, created.getStatus());

        QuantityChangeRequest consume = new QuantityChangeRequest();
        consume.setQuantity(new BigDecimal("2.5"));
        consume.setNote("prep");

        var updated = ingredientService.consumeIngredient(created.getId(), consume, employee);
        assertEquals(new BigDecimal("9.5"), updated.getQuantity());
        assertEquals(IngredientStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void createIngredientsBatch_shouldCreatePartsInSameLot() {
        IngredientBatchRequest request = new IngredientBatchRequest();
        request.setRestaurantId(restaurant.getId());
        request.setLotName("Pork");
        request.setCategory("meat");
        request.setUnit("kg");
        request.setExpiryDate(LocalDate.now().plusDays(5));
        request.setNotifyDaysBefore(2);

        IngredientBatchItemRequest shoulder = new IngredientBatchItemRequest();
        shoulder.setPartName("Shoulder");
        shoulder.setInitialQuantity(new BigDecimal("4.0"));
        shoulder.setQuantity(new BigDecimal("4.0"));

        IngredientBatchItemRequest belly = new IngredientBatchItemRequest();
        belly.setPartName("Belly");
        belly.setInitialQuantity(new BigDecimal("3.0"));
        belly.setQuantity(new BigDecimal("3.0"));

        request.setItems(List.of(shoulder, belly));

        var created = ingredientService.createIngredientsBatch(request, employee);

        assertEquals(2, created.size());
        assertEquals(created.get(0).getLotId(), created.get(1).getLotId());
        assertEquals("Pork", created.get(0).getLotName());
        assertEquals("Pork - Shoulder", created.get(0).getName());
        assertEquals("Pork - Belly", created.get(1).getName());
        assertEquals(LocalDate.now().plusDays(5), created.get(0).getExpiryDate());
        assertEquals(LocalDate.now().plusDays(5), created.get(1).getExpiryDate());
    }

    @Test
    void consumeAll_shouldMarkUsed() {
        IngredientRequest request = new IngredientRequest();
        request.setRestaurantId(restaurant.getId());
        request.setName("Fish sauce");
        request.setCategory("sauce");
        request.setInitialQuantity(new BigDecimal("3.0"));
        request.setQuantity(new BigDecimal("3.0"));
        request.setUnit("bottle");
        request.setExpiryDate(LocalDate.now().plusDays(30));
        request.setNotifyDaysBefore(7);

        var created = ingredientService.createIngredient(request, employee);

        QuantityChangeRequest consume = new QuantityChangeRequest();
        consume.setQuantity(new BigDecimal("3.0"));

        var updated = ingredientService.consumeIngredient(created.getId(), consume, employee);
        assertEquals(0, updated.getQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(IngredientStatus.USED, updated.getStatus());
    }

    @Test
    void employeeCannotAdjustQuantityDirectly() {
        IngredientRequest request = new IngredientRequest();
        request.setRestaurantId(restaurant.getId());
        request.setName("Sugar");
        request.setCategory("dry");
        request.setInitialQuantity(new BigDecimal("2.5"));
        request.setQuantity(new BigDecimal("2.5"));
        request.setUnit("kg");
        request.setExpiryDate(LocalDate.now().plusDays(10));
        request.setNotifyDaysBefore(3);

        var created = ingredientService.createIngredient(request, employee);

        QuantityAdjustmentRequest adjust = new QuantityAdjustmentRequest();
        adjust.setQuantity(new BigDecimal("2.0"));
        adjust.setReason("count correction");

        assertThrows(IllegalArgumentException.class,
                () -> ingredientService.adjustQuantity(created.getId(), adjust, employee));
    }
}
