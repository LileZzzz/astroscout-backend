package com.astroscout.backend.observation;

import com.astroscout.backend.common.AccessDeniedException;
import com.astroscout.backend.common.ObservationLogNotFoundException;
import com.astroscout.backend.user.User;
import com.astroscout.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/logs/{logId}/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final ObservationLogRepository observationLogRepository;
    private final UserRepository userRepository;

    public CommentController(
            CommentRepository commentRepository,
            ObservationLogRepository observationLogRepository,
            UserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.observationLogRepository = observationLogRepository;
        this.userRepository = userRepository;
    }

    public record CreateCommentRequest(@NotBlank String content) {}

    public record CommentResponse(
            Long id,
            Long userId,
            String username,
            String content,
            Instant createdAt
    ) {}

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            Authentication authentication,
            @PathVariable Long logId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found for username " + username));

        ObservationLog log = observationLogRepository.findById(logId)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + logId));

        Comment comment = new Comment(log, user, request.content());
        Comment saved = commentRepository.save(comment);

        CommentResponse response = toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(@PathVariable Long logId) {
        if (!observationLogRepository.existsById(logId)) {
            throw new ObservationLogNotFoundException("Observation log not found with id " + logId);
        }

        List<Comment> comments = commentRepository.findByLog_IdOrderByCreatedAtAsc(logId);
        List<CommentResponse> responses = comments.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count(@PathVariable Long logId) {
        if (!observationLogRepository.existsById(logId)) {
            throw new ObservationLogNotFoundException("Observation log not found with id " + logId);
        }

        long count = commentRepository.countByLog_Id(logId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long logId,
            @PathVariable Long commentId
    ) {
        String username = (String) authentication.getPrincipal();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found for username " + username));

        ObservationLog log = observationLogRepository.findById(logId)
                .orElseThrow(() -> new ObservationLogNotFoundException("Observation log not found with id " + logId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id " + commentId));

        if (!comment.getLog().getId().equals(log.getId())) {
            throw new IllegalArgumentException("Comment does not belong to the specified log");
        }

        boolean isCommentAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isLogOwner = log.getUser().getId().equals(currentUser.getId());
        if (!isCommentAuthor && !isLogOwner) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }

        commentRepository.delete(comment);
        return ResponseEntity.noContent().build();
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}

