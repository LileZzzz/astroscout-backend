package com.astroscout.backend.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 2000)
        String message
) {}
