package com.cloudpulse.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SloTargetRequest(
        @NotNull
        @DecimalMin("90.0")
        @DecimalMax("99.99")
        Double availabilitySloPercent) {
}

