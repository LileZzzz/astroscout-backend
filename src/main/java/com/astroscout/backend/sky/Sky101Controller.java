package com.astroscout.backend.sky;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sky101")
public class Sky101Controller {

    private final Sky101Service sky101Service;

    public Sky101Controller(Sky101Service sky101Service) {
        this.sky101Service = sky101Service;
    }

    @GetMapping("/scene")
    public ResponseEntity<Sky101SceneResponse> scene() {
        return ResponseEntity.ok(sky101Service.getScene());
    }

    @GetMapping("/objects")
    public ResponseEntity<List<Sky101Object>> listObjects() {
        return ResponseEntity.ok(sky101Service.getScene().objects());
    }
}
