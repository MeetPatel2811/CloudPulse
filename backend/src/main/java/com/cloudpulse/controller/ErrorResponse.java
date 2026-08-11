package com.cloudpulse.controller;

import java.time.Instant;

// Standard JSON body returned for every API error, so clients get one consistent shape
// (e.g. { "timestamp": ..., "status": 404, "error": "Not Found", "message": "..." }).
public record ErrorResponse(Instant timestamp, int status, String error, String message) {
}
