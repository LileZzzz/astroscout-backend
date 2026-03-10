package com.astroscout.backend.observation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final ObservationLogRepository observationLogRepository;

    public FeedController(ObservationLogRepository observationLogRepository) {
        this.observationLogRepository = observationLogRepository;
    }

    public record FeedItemResponse(
            Long id,
            Long userId,
            String title,
            String locationName,
            Instant observedAt,
            Boolean isPublic
    ) {}

    @GetMapping
    public ResponseEntity<List<FeedItemResponse>> getPublicFeed() {
        List<ObservationLog> logs = observationLogRepository.findByIsPublicTrueOrderByCreatedAtDesc();

        List<FeedItemResponse> items = logs.stream()
                .map(log -> new FeedItemResponse(
                        log.getId(),
                        log.getUser().getId(),
                        log.getTitle(),
                        log.getLocationName(),
                        log.getObservedAt(),
                        log.getIsPublic()
                ))
                .toList();

        return ResponseEntity.ok(items);
    }
}

