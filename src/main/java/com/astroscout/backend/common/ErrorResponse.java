package com.astroscout.backend.common;

import java.time.Instant;

public record ErrorResponse(
        int code,
        String message,
        Instant timestamp
) {
}

