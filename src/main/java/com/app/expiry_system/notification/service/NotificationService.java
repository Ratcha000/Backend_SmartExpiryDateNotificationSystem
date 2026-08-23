package com.app.expiry_system.notification.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.notification.dto.NotificationResponse;
import com.app.expiry_system.notification.entity.AppNotification;
import com.app.expiry_system.notification.entity.NotificationType;
import com.app.expiry_system.notification.repository.AppNotificationRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;

    public NotificationService(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public AppNotification create(String restaurantId, String userId, NotificationType type, String title, String message) {
        AppNotification notification = new AppNotification();
        notification.setRestaurantId(restaurantId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public List<NotificationResponse> getNotifications(AppUser currentUser) {
        return notificationRepository.findByUserId(currentUser.getId()).stream()
                .sorted(Comparator.comparing(AppNotification::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse markRead(String id, AppUser currentUser) {
        AppNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to access this notification");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRestaurantId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
