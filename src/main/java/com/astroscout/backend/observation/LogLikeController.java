package com.astroscout.backend.observation;

import com.astroscout.backend.common.AccessDeniedException;
import com.astroscout.backend.common.ObservationLogNotFoundException;
import com.astroscout.backend.user.User;
import com.astroscout.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogLikeController {

    private final ObservationLogRepository observationLogRepository;
    private final LogLikeRepository logLikeRepository;
    private final UserRepository userRepository;

    public LogLikeController(
            ObservationLogRepository observationLogRepository,
            LogLikeRepository logLikeRepository,
            UserRepository userRepository
    ) {
        this.observationLogRepository = observationLogRepository;
        this.logLikeRepository = logLikeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{logId}/like")
    public ResponseEntity<Void> like(
            Authentication authentication,
            @PathVariable Long logId
    ) {
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found for username " + username));

        ObservationLog log = observationLogRepository.findById(logId)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + logId));

        boolean alreadyLiked = logLikeRepository.findByLog_IdAndUser_Id(log.getId(), user.getId()).isPresent();
        if (alreadyLiked) {
            throw new AccessDeniedException("You have already liked this log");
        }

        LogLike like = new LogLike(log, user);
        logLikeRepository.save(like);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{logId}/like")
    public ResponseEntity<Void> unlike(
            Authentication authentication,
            @PathVariable Long logId
    ) {
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found for username " + username));

        ObservationLog log = observationLogRepository.findById(logId)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + logId));

        logLikeRepository.findByLog_IdAndUser_Id(log.getId(), user.getId())
                .ifPresent(logLikeRepository::delete);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{logId}/likes/count")
    public ResponseEntity<Long> count(@PathVariable Long logId) {
        if (!observationLogRepository.existsById(logId)) {
            throw new ObservationLogNotFoundException("Observation log not found with id " + logId);
        }
        long count = logLikeRepository.countByLog_Id(logId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{logId}/likes/me")
    public ResponseEntity<LikedResponse> me(
            Authentication authentication,
            @PathVariable Long logId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(new LikedResponse(false));
        }
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new LikedResponse(false));
        }
        if (!observationLogRepository.existsById(logId)) {
            throw new ObservationLogNotFoundException("Observation log not found with id " + logId);
        }
        boolean liked = logLikeRepository.findByLog_IdAndUser_Id(logId, user.getId()).isPresent();
        return ResponseEntity.ok(new LikedResponse(liked));
    }

    public record LikedResponse(boolean liked) {}
}

