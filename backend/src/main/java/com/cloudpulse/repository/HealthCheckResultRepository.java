package com.cloudpulse.repository;

import com.cloudpulse.model.HealthCheckResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HealthCheckResultRepository extends JpaRepository<HealthCheckResult, UUID> {
    List<HealthCheckResult> findByService_IdOrderByCheckedAtDesc(UUID serviceId);

    List<HealthCheckResult> findByService_IdAndCheckedAtBetweenOrderByCheckedAtDesc(
            UUID serviceId,
            Instant from,
            Instant to,
            Pageable pageable);

    long countByService_IdAndCheckedAtBetween(UUID serviceId, Instant from, Instant to);

    long countByService_IdAndReachableTrueAndCheckedAtBetween(
            UUID serviceId,
            Instant from,
            Instant to);

    @Query("""
            select avg(result.responseTimeMs)
            from HealthCheckResult result
            where result.service.id = :serviceId
              and result.reachable = true
              and result.checkedAt between :from and :to
            """)
    Double averageSuccessfulResponseTimeMs(
            @Param("serviceId") UUID serviceId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    List<HealthCheckResult> findByService_IdAndReachableTrueAndCheckedAtBetweenOrderByCheckedAtDesc(
            UUID serviceId,
            Instant from,
            Instant to,
            Pageable pageable);
}
