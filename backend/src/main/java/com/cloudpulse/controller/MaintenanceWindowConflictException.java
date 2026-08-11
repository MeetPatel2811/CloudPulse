package com.cloudpulse.controller;

/**
 * Raised when a new maintenance window would overlap one that already exists for the
 * same service. Mapped to a 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class MaintenanceWindowConflictException extends RuntimeException {
    public MaintenanceWindowConflictException(String message) {
        super(message);
    }
}
