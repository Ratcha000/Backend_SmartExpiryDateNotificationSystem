package com.app.expiry_system.notification.repository;

import com.app.expiry_system.notification.entity.AppNotification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {

    List<AppNotification> findByUserId(String userId);
}
