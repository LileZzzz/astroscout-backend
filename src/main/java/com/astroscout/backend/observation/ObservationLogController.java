package com.astroscout.backend.observation;

import com.astroscout.backend.common.AccessDeniedException;
import com.astroscout.backend.common.ObservationLogNotFoundException;
import com.astroscout.backend.user.User;
import com.astroscout.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class ObservationLogController {

    private final ObservationLogRepository observationLogRepository;
    private final UserRepository userRepository;

    public ObservationLogController(
            ObservationLogRepository observationLogRepository,
            UserRepository userRepository
    ) {
        this.observationLogRepository = observationLogRepository;
        this.userRepository = userRepository;
    }

    public record CreateLogRequest(
            @NotBlank String title,
            String description,
            @NotNull Instant observedAt,
            String locationName,
            @NotNull Double lat,
            @NotNull Double lng,
            Integer bortleScale,
            String weatherCondition,
            Integer seeingRating,
            Boolean isPublic
    ) {}

    public record UpdateLogRequest(
            @NotBlank String title,
            String description,
            @NotNull Instant observedAt,
            String locationName,
            @NotNull Double lat,
            @NotNull Double lng,
            Integer bortleScale,
            String weatherCondition,
            Integer seeingRating,
            Boolean isPublic
    ) {}

    public record ObservationLogResponse(
            Long id,
            Long userId,
            String title,
            String description,
            Instant observedAt,
            String locationName,
            Double lat,
            Double lng,
            Integer bortleScale,
            String weatherCondition,
            Integer seeingRating,
            Boolean isPublic,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Response for single log detail; includes username for display. */
    public record LogDetailResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String description,
            Instant observedAt,
            String locationName,
            Double lat,
            Double lng,
            Integer bortleScale,
            String weatherCondition,
            Integer seeingRating,
            Boolean isPublic,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @PostMapping
    public ResponseEntity<ObservationLogResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateLogRequest request
    ) {
        if (request.lat() < -90.0 || request.lat() > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (request.lng() < -180.0 || request.lng() > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }

        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));

        ObservationLog log = new ObservationLog();
        log.setUser(user);
        log.setTitle(request.title());
        log.setDescription(request.description());
        log.setObservedAt(request.observedAt());
        log.setLocationName(request.locationName());
        log.setLat(request.lat());
        log.setLng(request.lng());
        log.setBortleScale(request.bortleScale());
        log.setWeatherCondition(request.weatherCondition());
        log.setSeeingRating(request.seeingRating());
        log.setIsPublic(request.isPublic() != null ? request.isPublic() : Boolean.TRUE);

        ObservationLog saved = observationLogRepository.save(log);
        ObservationLogResponse response = toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ObservationLogResponse>> listForCurrentUser(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));

        List<ObservationLog> logs = observationLogRepository.findByUser_Id(user.getId());
        List<ObservationLogResponse> responses = logs.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogDetailResponse> getById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        ObservationLog log = observationLogRepository.findById(id)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + id));

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        if (isAuthenticated) {
            String email = (String) authentication.getPrincipal();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));
            boolean isOwner = log.getUser().getId().equals(currentUser.getId());
            boolean isPublic = Boolean.TRUE.equals(log.getIsPublic());
            if (!isOwner && !isPublic) {
                throw new AccessDeniedException("You are not allowed to view this log");
            }
        } else {
            if (!Boolean.TRUE.equals(log.getIsPublic())) {
                throw new ObservationLogNotFoundException("Observation log not found with id " + id);
            }
        }

        LogDetailResponse response = toDetailResponse(log);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogDetailResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogRequest request
    ) {
        if (request.lat() < -90.0 || request.lat() > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (request.lng() < -180.0 || request.lng() > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }

        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));

        ObservationLog log = observationLogRepository.findById(id)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + id));

        if (!log.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to edit this log");
        }

        log.setTitle(request.title());
        log.setDescription(request.description());
        log.setObservedAt(request.observedAt());
        log.setLocationName(request.locationName());
        log.setLat(request.lat());
        log.setLng(request.lng());
        log.setBortleScale(request.bortleScale());
        log.setWeatherCondition(request.weatherCondition());
        log.setSeeingRating(request.seeingRating());
        log.setIsPublic(request.isPublic() != null ? request.isPublic() : Boolean.TRUE);

        ObservationLog saved = observationLogRepository.save(log);
        return ResponseEntity.ok(toDetailResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));

        ObservationLog log = observationLogRepository.findById(id)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + id));

        if (!log.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to delete this log");
        }

        observationLogRepository.delete(log);
        return ResponseEntity.noContent().build();
    }

    private ObservationLogResponse toResponse(ObservationLog log) {
        return new ObservationLogResponse(
                log.getId(),
                log.getUser().getId(),
                log.getTitle(),
                log.getDescription(),
                log.getObservedAt(),
                log.getLocationName(),
                log.getLat(),
                log.getLng(),
                log.getBortleScale(),
                log.getWeatherCondition(),
                log.getSeeingRating(),
                log.getIsPublic(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    private LogDetailResponse toDetailResponse(ObservationLog log) {
        String username = log.getUser().getUsername() != null
                ? log.getUser().getUsername()
                : "User " + log.getUser().getId();
        return new LogDetailResponse(
                log.getId(),
                log.getUser().getId(),
                username,
                log.getTitle(),
                log.getDescription(),
                log.getObservedAt(),
                log.getLocationName(),
                log.getLat(),
                log.getLng(),
                log.getBortleScale(),
                log.getWeatherCondition(),
                log.getSeeingRating(),
                log.getIsPublic(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}

