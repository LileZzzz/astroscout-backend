package com.astroscout.backend.observation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/observe")
public class ObserveController {

    private final ObservationScoreService observationScoreService;
    private final CelestialCatalogService celestialCatalogService;
    private final BestWindowService bestWindowService;

        public ObserveController(ObservationScoreService observationScoreService,
                                 CelestialCatalogService celestialCatalogService,
                                 BestWindowService bestWindowService) {
            this.observationScoreService = observationScoreService;
            this.celestialCatalogService = celestialCatalogService;
            this.bestWindowService = bestWindowService;
        }

    public record ScoreResponse(
            double score,
            String grade,
            double cloudScore,
            double moonScore,
            double lightPollutionScore,
            double visibilityScore,
            ObservationScoreService.WeatherData weather,
            ObservationScoreService.AstronomyData astronomy,
            ObservationScoreService.LightPollutionData lightPollution
    ) {}

    public record CelestialObjectResponse(
            String name,
            String type,
            double alt,
            double az,
            double magnitude,
            boolean needsTelescope,
            String description
    ) {}

    public record BestWindowResponse(
            LocalTime start,
            LocalTime end,
            String quality,
            String reason
    ) {}

    @GetMapping("/score")
    public ResponseEntity<ScoreResponse> score(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }

        var breakdown = observationScoreService.score(lat, lng, date);
        ScoreResponse response = new ScoreResponse(
                breakdown.totalScore(),
                breakdown.grade(),
                breakdown.cloudScore(),
                breakdown.moonScore(),
                breakdown.lightPollutionScore(),
                breakdown.visibilityScore(),
                breakdown.weather(),
                breakdown.astronomy(),
                breakdown.lightPollution()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/best-window")
    public ResponseEntity<List<BestWindowResponse>> bestWindow(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }

        var windows = bestWindowService.computeBestWindows(lat, lng, date);
        var response = windows.stream()
                .map(w -> new BestWindowResponse(
                        w.start(),
                        w.end(),
                        w.quality(),
                        w.reason()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/celestial")
    public ResponseEntity<List<CelestialObjectResponse>> celestial(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }

        var visible = celestialCatalogService.computeVisible(lat, lng, date, time);
        var response = visible.stream()
                .map(v -> new CelestialObjectResponse(
                        v.name(),
                        v.type(),
                        v.altDeg(),
                        v.azDeg(),
                        v.magnitude(),
                        v.needsTelescope(),
                        v.description()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}

