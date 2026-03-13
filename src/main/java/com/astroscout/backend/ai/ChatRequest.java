package com.astroscout.backend.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 2000)
        String message,
        Double lat,
        Double lng,
        String date,
        Double score,
        @Size(max = 500)
        String weatherSummary,
        @Size(max = 120)
        String moonPhaseLabel,
        Integer bortleScale,
        @Size(max = 1200)
        String targetSummary
) {}
