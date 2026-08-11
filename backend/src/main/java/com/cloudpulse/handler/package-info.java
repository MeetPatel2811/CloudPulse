/*
 * Chain of Responsibility for processing health-check results.
 *
 * After a monitor produces a HealthCheckResult, it is passed through a chain of
 * small handlers, in this order:
 *   1. ResponseValidationHandler - reject results we can't process
 *   2. AvailabilityHandler       - unreachable or HTTP error means DOWN
 *   3. LatencyHandler            - fast means HEALTHY, slow means DEGRADED
 *   4. StatusChangeHandler       - save a StatusEvent when the status changes
 *   5. AlertCreationHandler      - raise or resolve alerts
 *
 * HealthCheckProcessor builds the chain and is the single entry point.
 */
package com.cloudpulse.handler;
