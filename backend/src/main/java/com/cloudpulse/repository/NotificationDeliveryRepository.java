package com.cloudpulse.repository;

import com.cloudpulse.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    List<NotificationDelivery> findByOrderBySentAtDesc();
    List<NotificationDelivery> findByAlertIdOrderBySentAtDesc(UUID alertId);
}
