package com.astroscout.backend.observation;

import com.astroscout.backend.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "log_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"log_id", "user_id"})
)
public class LogLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private ObservationLog log;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LogLike() {
    }

    public LogLike(ObservationLog log, User user) {
        this.log = log;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ObservationLog getLog() {
        return log;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

