package com.cloudpulse.repository;

import com.cloudpulse.model.WebhookTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookTargetRepository extends JpaRepository<WebhookTarget, UUID> {
    List<WebhookTarget> findByEnabledTrue();
}
