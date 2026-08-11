package com.cloudpulse.controller;

/**
 * Raised when a service cannot be deleted because it still has monitoring history
 * (health-check results, status events, or alerts referencing it). Mapped to a
 * 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class ServiceHasHistoryException extends RuntimeException {
    public ServiceHasHistoryException(String message) {
        super(message);
    }
}
