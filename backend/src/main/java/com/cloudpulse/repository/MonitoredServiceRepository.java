package com.cloudpulse.repository;

import com.cloudpulse.model.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, UUID> {
    List<MonitoredService> findByEnabledTrue();
}