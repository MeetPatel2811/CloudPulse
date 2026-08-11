package com.cloudpulse.repository;

import com.cloudpulse.model.MaintenanceWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, UUID> {

    List<MaintenanceWindow> findByService_IdOrderByStartsAtDesc(UUID serviceId);

    /** Any window for this service that overlaps [startsAt, endsAt). */
    List<MaintenanceWindow> findByService_IdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID serviceId, Instant endsAt, Instant startsAt);

    /** Whether the service has a window covering the given instant (used to suppress alerts). */
    boolean existsByService_IdAndStartsAtLessThanEqualAndEndsAtGreaterThan(
            UUID serviceId, Instant now1, Instant now2);
}
