package com.cloudpulse.repository;

import com.cloudpulse.model.IncidentActivity;
import com.cloudpulse.model.IncidentActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentActivityRepository extends JpaRepository<IncidentActivity, UUID> {

    // The full timeline for one incident, oldest first.
    List<IncidentActivity> findByAlert_IdOrderByCreatedAtAsc(UUID alertId);

    // The most recent entry of a given type (used to derive the current owner from
    // the latest ASSIGNED entry).
    Optional<IncidentActivity> findFirstByAlert_IdAndTypeOrderByCreatedAtDesc(
            UUID alertId, IncidentActivityType type);
}
