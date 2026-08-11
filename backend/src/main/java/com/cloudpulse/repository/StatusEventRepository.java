package com.cloudpulse.repository;

import com.cloudpulse.model.StatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StatusEventRepository extends JpaRepository<StatusEvent, UUID> {
    List<StatusEvent> findByService_IdOrderByOccurredAtDesc(UUID serviceId);
}