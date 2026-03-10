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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public ResponseEntity<ObservationLogResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateLogRequest request
    ) {
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
    public ResponseEntity<ObservationLogResponse> getById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email " + email));

        ObservationLog log = observationLogRepository.findById(id)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + id));

        boolean isOwner = log.getUser().getId().equals(currentUser.getId());
        boolean isPublic = Boolean.TRUE.equals(log.getIsPublic());
        if (!isOwner && !isPublic) {
            throw new AccessDeniedException("You are not allowed to view this log");
        }

        ObservationLogResponse response = toResponse(log);
        return ResponseEntity.ok(response);
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
}

