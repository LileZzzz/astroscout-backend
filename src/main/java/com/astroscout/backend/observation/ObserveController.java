package com.astroscout.backend.observation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/observe")
public class ObserveController {

    private final ObservationScoreService observationScoreService;

    public ObserveController(ObservationScoreService observationScoreService) {
        this.observationScoreService = observationScoreService;
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

    @GetMapping("/score")
    public ResponseEntity<ScoreResponse> score(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
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
}

