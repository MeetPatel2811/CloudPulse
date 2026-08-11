package com.cloudpulse.notification;

// The outcome of one webhook POST: whether it succeeded, the HTTP status (0 if the
// request never completed), and a short error message when it failed.
public record DeliveryResult(boolean success, int httpStatus, String error) {
}
