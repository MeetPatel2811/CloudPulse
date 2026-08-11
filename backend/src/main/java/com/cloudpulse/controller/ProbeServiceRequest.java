package com.cloudpulse.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProbeServiceRequest(
        @NotBlank
        @Size(max = 2_048)
        String url
) {
}
