package com.cloudpulse.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body for assigning an incident owner. Blank or oversized input becomes a 400 via
// GlobalExceptionHandler (255 matches the default owner-name column length).
public record AssignOwnerRequest(@NotBlank @Size(max = 255) String owner) {
}
