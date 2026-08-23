package com.app.expiry_system.notification.dto;

import com.app.expiry_system.notification.entity.NotificationType;
import java.time.Instant;

public class NotificationResponse {

    private String id;
    private String restaurantId;
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private Instant createdAt;

    public NotificationResponse(String id, String restaurantId, String userId, NotificationType type,
                                String title, String message, boolean read, Instant createdAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getRestaurantId() { return restaurantId; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
