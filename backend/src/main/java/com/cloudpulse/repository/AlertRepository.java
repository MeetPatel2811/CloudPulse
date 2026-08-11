package com.cloudpulse.repository;

import com.cloudpulse.model.Alert;
import com.cloudpulse.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<Alert> findByService_IdOrderByCreatedAtDesc(UUID serviceId);
}