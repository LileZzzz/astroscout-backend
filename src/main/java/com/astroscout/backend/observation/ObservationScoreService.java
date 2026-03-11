package com.astroscout.backend.observation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ObservationScoreService {

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
        // TODO: replace stubs with real external API calls
        WeatherData weather = stubWeather(lat, lng, date);
        AstronomyData astronomy = stubAstronomy(lat, lng, date);
        LightPollutionData lightPollution = stubLightPollution(lat, lng);

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

    private WeatherData stubWeather(double lat, double lng, LocalDate date) {
        // Simple stub: slightly better weather at lower latitudes
        int clouds = 20;
        double visibility = 15.0;
        int humidity = 40;
        double wind = 3.0;
        return new WeatherData(clouds, visibility, humidity, wind);
    }

    private AstronomyData stubAstronomy(double lat, double lng, LocalDate date) {
        // Simple stub: alternate between new and quarter moon based on day of month
        int day = date.getDayOfMonth();
        double phase = switch (day % 4) {
            case 0 -> 0.0;  // new
            case 1 -> 0.25; // first quarter
            case 2 -> 0.5;  // half
            default -> 0.75; // gibbous
        };
        String label = switch (day % 4) {
            case 0 -> "New Moon";
            case 1 -> "First Quarter";
            case 2 -> "Half Moon";
            default -> "Gibbous Moon";
        };
        return new AstronomyData(phase, label);
    }

    private LightPollutionData stubLightPollution(double lat, double lng) {
        // Simple stub: treat everything as Bortle 3
        return new LightPollutionData(3);
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

