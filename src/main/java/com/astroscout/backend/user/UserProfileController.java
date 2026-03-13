package com.astroscout.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record UserProfileResponse(
            Long id,
            String username,
            String role
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 50) String username
    ) {}

    public record UpdatePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        User user = findCurrentUser(authentication);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        User user = findCurrentUser(authentication);

        userRepository.findByUsername(request.username())
                .filter(found -> !found.getId().equals(user.getId()))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Username already in use");
                });

        user.setUsername(request.username().trim());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request
    ) {
        User user = findCurrentUser(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private User findCurrentUser(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found for username " + username));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
