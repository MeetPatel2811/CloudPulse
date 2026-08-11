package com.cloudpulse.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body for adding a note to an incident. Blank or oversized input becomes a 400 via
// GlobalExceptionHandler (2000 matches the incident_activity.detail column length).
// The author is optional; the controller falls back to a default when it is missing.
public record AddNoteRequest(@NotBlank @Size(max = 2000) String note, @Size(max = 255) String author) {
}
