package com.astroscout.backend.observation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final ObservationLogRepository observationLogRepository;

    public FeedController(ObservationLogRepository observationLogRepository) {
        this.observationLogRepository = observationLogRepository;
    }

    public record FeedItemResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String locationName,
            Instant observedAt,
            Boolean isPublic
    ) {}

    public record FeedPageResponse(List<FeedItemResponse> content, boolean hasMore) {}

    @GetMapping
    public ResponseEntity<FeedPageResponse> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size
    ) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = observationLogRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);

        List<FeedItemResponse> items = pageResult.getContent().stream()
                .map(log -> new FeedItemResponse(
                        log.getId(),
                        log.getUser().getId(),
                        log.getUser().getUsername() != null ? log.getUser().getUsername() : "User " + log.getUser().getId(),
                        log.getTitle(),
                        log.getLocationName(),
                        log.getObservedAt(),
                        log.getIsPublic()
                ))
                .toList();

        return ResponseEntity.ok(new FeedPageResponse(items, pageResult.hasNext()));
    }
}

