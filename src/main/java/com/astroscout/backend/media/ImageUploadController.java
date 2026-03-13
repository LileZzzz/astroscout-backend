package com.astroscout.backend.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {

    private static final long MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${astroscout.upload-dir:uploads}")
    private String uploadDir;

    public record UploadImageResponse(String url) {}

    @PostMapping("/image")
    public ResponseEntity<UploadImageResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image size must be <= 8MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image upload is supported");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image type");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(filename).normalize();

        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store image", e);
        }

        String url = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/uploads/")
            .path(filename)
            .toUriString();
        return ResponseEntity.ok(new UploadImageResponse(url));
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new IllegalArgumentException("Image extension is missing");
        }
        return filename.substring(idx + 1).toLowerCase();
    }
}
