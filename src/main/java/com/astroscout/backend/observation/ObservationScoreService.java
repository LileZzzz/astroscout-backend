package com.astroscout.backend.observation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ObservationScoreService {

    private final OpenMeteoWeatherService weatherService;
    private final LightPollutionService lightPollutionService;

    public ObservationScoreService(OpenMeteoWeatherService weatherService,
                                   LightPollutionService lightPollutionService) {
        this.weatherService = weatherService;
        this.lightPollutionService = lightPollutionService;
    }

    public record WeatherData(
            int cloudCoverPercent, // 0-100
            double visibilityKm,   // e.g. 0-20
            int humidityPercent,   // 0-100
            double windSpeedMps    // m/s
    ) {}

    public record AstronomyData(
            double moonPhase,      // 0.0 new moon, 1.0 full moon
            String moonPhaseLabel
    ) {}

    public record LightPollutionData(
            int bortleScale        // 1-9
    ) {}

    public record ScoreBreakdown(
            double totalScore,
            String grade,
            double cloudScore,
            double moonScore,
            double lightPollutionScore,
            double visibilityScore,
            WeatherData weather,
            AstronomyData astronomy,
            LightPollutionData lightPollution
    ) {}

    public ScoreBreakdown score(double lat, double lng, LocalDate date) {
        WeatherData weather = weatherService.getWeather(lat, lng, date);
        AstronomyData astronomy = computeAstronomyFromDate(date);
        LightPollutionData lightPollution = lightPollutionService.getLightPollution(lat, lng);

        double cloudScore = computeCloudScore(weather.cloudCoverPercent());
        double moonScore = computeMoonScore(astronomy.moonPhase());
        double lightScore = computeLightPollutionScore(lightPollution.bortleScale());
        double visibilityScore = computeVisibilityScore(weather.visibilityKm());

        double total = cloudScore * 0.30
                + moonScore * 0.25
                + lightScore * 0.25
                + visibilityScore * 0.20;

        String grade = grade(total);

        return new ScoreBreakdown(
                total,
                grade,
                cloudScore,
                moonScore,
                lightScore,
                visibilityScore,
                weather,
                astronomy,
                lightPollution
        );
    }

    public AstronomyData astronomyForDate(LocalDate date) {
        return computeAstronomyFromDate(date);
    }

    private AstronomyData computeAstronomyFromDate(LocalDate date) {
        // Simple local moon phase approximation:
        // Reference new moon: 2000-01-06 (common reference in algorithms),
        // synodic month ~ 29.530588 days.
        LocalDate reference = LocalDate.of(2000, 1, 6);
        long daysSince = reference.until(date).getDays();
        double synodicMonth = 29.530588;
        double phase = (daysSince % synodicMonth) / synodicMonth;
        if (phase < 0) {
            phase += 1.0;
        }
        double normalized = Math.max(0.0, Math.min(1.0, phase));
        String label;
        if (normalized < 0.0625 || normalized >= 0.9375) {
            label = "New Moon";
        } else if (normalized < 0.25) {
            label = "Waxing Crescent";
        } else if (normalized < 0.3125) {
            label = "First Quarter";
        } else if (normalized < 0.5) {
            label = "Waxing Gibbous";
        } else if (normalized < 0.5625) {
            label = "Full Moon";
        } else if (normalized < 0.75) {
            label = "Waning Gibbous";
        } else if (normalized < 0.8125) {
            label = "Last Quarter";
        } else {
            label = "Waning Crescent";
        }
        return new AstronomyData(normalized, label);
    }

    private double computeCloudScore(int cloudPercent) {
        // 0% clouds -> 100, 100% clouds -> 0
        int clamped = Math.min(100, Math.max(0, cloudPercent));
        return 100.0 * (1.0 - clamped / 100.0);
    }

    private double computeMoonScore(double moonPhase) {
        // 0.0 new moon -> 100, 1.0 full moon -> 0, roughly linear
        double clamped = Math.max(0.0, Math.min(1.0, moonPhase));
        return 100.0 * (1.0 - clamped);
    }

    private double computeLightPollutionScore(int bortle) {
        // Bortle 1 -> 100, 9 -> 0
        int clamped = Math.min(9, Math.max(1, bortle));
        return 100.0 * (1.0 - (clamped - 1) / 8.0);
    }

    private double computeVisibilityScore(double visibilityKm) {
        // >10km -> 100, <1km -> 0, linear in between
        double clamped = Math.max(0.0, Math.min(20.0, visibilityKm));
        if (clamped <= 1.0) return 0.0;
        if (clamped >= 10.0) return 100.0;
        return 100.0 * (clamped - 1.0) / 9.0;
    }

    private String grade(double score) {
        if (score >= 90.0) return "Excellent";
        if (score >= 70.0) return "Good";
        if (score >= 50.0) return "Fair";
        return "Poor";
    }
}

