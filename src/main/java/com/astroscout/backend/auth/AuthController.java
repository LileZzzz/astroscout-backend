package com.astroscout.backend.auth;

import com.astroscout.backend.security.JwtUtil;
import com.astroscout.backend.user.User;
import com.astroscout.backend.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            Long userId,
            String username,
            String token,
            String message
    ) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("Username already in use");
        }

        String hash = passwordEncoder.encode(req.password());
        User user = new User(hash, req.username());
        User saved = userRepository.save(user);

        AuthResponse response = new AuthResponse(
                saved.getId(),
                saved.getUsername(),
                null,
                "REGISTERED"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = JwtUtil.generateToken(user);

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getUsername(),
                token,
                "LOGGED_IN"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo")
    public ResponseEntity<AuthResponse> demoLogin() {
        final String demoUsername = "DemoExplorer";
        User user = userRepository.findByUsername(demoUsername)
                .orElseGet(() -> {
                    String hash = passwordEncoder.encode("demo-" + UUID.randomUUID());
                    User demoUser = new User(hash, demoUsername);
                    return userRepository.save(demoUser);
                });

        String token = JwtUtil.generateToken(user);
        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getUsername(),
                token,
                "DEMO_LOGGED_IN"
        );
        return ResponseEntity.ok(response);
    }
}

