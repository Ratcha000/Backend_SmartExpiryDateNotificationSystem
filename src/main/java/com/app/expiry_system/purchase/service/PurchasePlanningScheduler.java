package com.app.expiry_system.purchase.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.auth.entity.UserRole;
import com.app.expiry_system.auth.repository.AppUserRepository;
import com.app.expiry_system.notification.entity.NotificationType;
import com.app.expiry_system.notification.service.NotificationService;
import com.app.expiry_system.purchase.entity.PurchaseRunSource;
import com.app.expiry_system.purchase.entity.RestaurantPurchaseSetting;
import com.app.expiry_system.purchase.repository.RestaurantPurchaseSettingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PurchasePlanningScheduler {

    private final RestaurantPurchaseSettingRepository settingRepository;
    private final PurchasePlanningService purchasePlanningService;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    public PurchasePlanningScheduler(RestaurantPurchaseSettingRepository settingRepository,
                                     PurchasePlanningService purchasePlanningService,
                                     AppUserRepository appUserRepository,
                                     NotificationService notificationService) {
        this.settingRepository = settingRepository;
        this.purchasePlanningService = purchasePlanningService;
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Bangkok")
    public void runScheduledPurchasePlanning() {
        LocalDate today = LocalDate.now(PurchasePlanningService.PURCHASE_ZONE);
        LocalTime now = LocalTime.now(PurchasePlanningService.PURCHASE_ZONE).withSecond(0).withNano(0);

        settingRepository.findAll().stream()
                .filter(setting -> now.equals(setting.getNotificationTime()))
                .filter(setting -> purchasePlanningService.isPurchaseDay(setting, today))
                .forEach(setting -> runForSetting(setting, today));
    }

    private void runForSetting(RestaurantPurchaseSetting setting, LocalDate today) {
        List<AppUser> managers = appUserRepository.findByRestaurantId(setting.getRestaurantId()).stream()
                .filter(user -> user.getRole() == UserRole.MANAGER)
                .toList();
        if (managers.isEmpty()) {
            return;
        }

        try {
            int count = purchasePlanningService
                    .generateForRestaurant(setting.getRestaurantId(), today, PurchaseRunSource.SCHEDULED)
                    .size();
            managers.forEach(manager -> notificationService.create(
                    setting.getRestaurantId(),
                    manager.getId(),
                    NotificationType.PURCHASE_RECOMMENDATION,
                    "รายการซื้อวัตถุดิบวันนี้",
                    "มีรายการแนะนำ " + count + " รายการสำหรับรอบซื้อวันนี้"
            ));
        } catch (Exception exception) {
            managers.forEach(manager -> notificationService.create(
                    setting.getRestaurantId(),
                    manager.getId(),
                    NotificationType.PURCHASE_RECOMMENDATION_FAILED,
                    "สร้างรายการซื้อวัตถุดิบไม่สำเร็จ",
                    exception.getMessage()
            ));
        }
    }
}
