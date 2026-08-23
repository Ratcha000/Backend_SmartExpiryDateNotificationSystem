package com.app.expiry_system.ocr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.ocr.dto.OcrScanRequest;
import com.app.expiry_system.restaurant.entity.Restaurant;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
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
class OcrScanServiceIntegrationTest {

    @Autowired
    private OcrScanService ocrScanService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private AppUser employee;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setName("OCR Test Kitchen");
        restaurant.setManagerId("manager-seed");
        restaurant = restaurantRepository.save(restaurant);

        employee = new AppUser();
        employee.setEmail("ocr-employee@test.com");
        employee.setPasswordHash("hash");
        employee.setDisplayName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setRestaurantId(restaurant.getId());
        employee = appUserRepository.save(employee);
    }

    @Test
    void extractExpiryDate_shouldParseDmyDateNearExpKeyword() {
        LocalDate future = LocalDate.now().plusYears(1);
        String printed = String.format("%02d/%02d/%d", future.getDayOfMonth(), future.getMonthValue(), future.getYear());

        var response = ocrScanService.extractExpiryDate(request("EXP " + printed + "\nChicken Breast"), employee);

        assertEquals(future, response.getExpiryDate());
        assertEquals(printed, response.getMatchedText());
        assertEquals("HIGH", response.getConfidence());
        assertEquals(employee.getId(), response.getScannedBy());
    }

    @Test
    void extractExpiryDate_shouldParseYmdDate() {
        var response = ocrScanService.extractExpiryDate(request("expiry date 2026-08-09"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
        assertEquals("2026-08-09", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldParseTwoDigitYear() {
        var response = ocrScanService.extractExpiryDate(request("BBF 09-08-26"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
        assertEquals("09-08-26", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldParseMonthYearAsEndOfMonth() {
        var response = ocrScanService.extractExpiryDate(request("EXP 08/2026"), employee);

        assertEquals(LocalDate.of(2026, 8, 31), response.getExpiryDate());
        assertEquals("08/2026", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldParseTextMonth() {
        var response = ocrScanService.extractExpiryDate(request("EXP 09 AUG 2026"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
        assertEquals("09 AUG 2026", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldPreferExpOverMfg() {
        var response = ocrScanService.extractExpiryDate(request("MFG 01/08/2026\nEXP 09/08/2026"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
        assertEquals("09/08/2026", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldParseCompactDigitsWithoutSeparator() {
        var response = ocrScanService.extractExpiryDate(request("co/co\nMFG200526th115\nEXP200529"), employee);

        assertEquals(LocalDate.of(2029, 5, 20), response.getExpiryDate());
        assertEquals("200529", response.getMatchedText());
    }

    @Test
    void extractExpiryDate_shouldParseCompactEightDigits() {
        var response = ocrScanService.extractExpiryDate(request("EXP20260809"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
    }

    @Test
    void extractExpiryDate_shouldIgnoreCompactDigitsWithoutDateKeyword() {
        var response = ocrScanService.extractExpiryDate(request("LOT 200526 line A"), employee);

        assertNull(response.getExpiryDate());
        assertEquals("LOW", response.getConfidence());
    }

    @Test
    void extractExpiryDate_shouldConvertBuddhistYear() {
        var response = ocrScanService.extractExpiryDate(request("วันหมดอายุ 09/08/2569"), employee);

        assertEquals(LocalDate.of(2026, 8, 9), response.getExpiryDate());
    }

    @Test
    void extractExpiryDate_shouldFlagMfgOnlyDateAsLowConfidence() {
        var response = ocrScanService.extractExpiryDate(request("MFG200526"), employee);

        assertEquals(LocalDate.of(2026, 5, 20), response.getExpiryDate());
        assertEquals("LOW", response.getConfidence());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("วันที่ผลิต")));
    }

    @Test
    void extractExpiryDate_shouldWarnWhenDateIsPast() {
        var response = ocrScanService.extractExpiryDate(request("EXP 01/01/2026"), employee);

        assertEquals(LocalDate.of(2026, 1, 1), response.getExpiryDate());
        assertTrue(response.getWarnings().contains("วันที่หมดอายุผ่านไปแล้ว"));
    }

    @Test
    void extractExpiryDate_shouldReturnLowConfidenceWhenNoDateFound() {
        var response = ocrScanService.extractExpiryDate(request("Chicken Breast no date on label"), employee);

        assertNull(response.getExpiryDate());
        assertNull(response.getMatchedText());
        assertEquals("LOW", response.getConfidence());
        assertEquals(1, response.getWarnings().size());
    }

    @Test
    void extractExpiryDate_shouldRejectOtherRestaurant() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setName("Other Kitchen");
        otherRestaurant.setManagerId("other-manager");
        otherRestaurant = restaurantRepository.save(otherRestaurant);

        OcrScanRequest request = request("EXP 09/08/2026");
        request.setRestaurantId(otherRestaurant.getId());

        assertThrows(IllegalArgumentException.class,
                () -> ocrScanService.extractExpiryDate(request, employee));
    }

    private OcrScanRequest request(String rawText) {
        OcrScanRequest request = new OcrScanRequest();
        request.setRestaurantId(restaurant.getId());
        request.setRawText(rawText);
        request.setSource("CAMERA");
        return request;
    }
}
