package com.astroscout.backend.observation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BestWindowService {

    public record BestWindow(
            LocalTime start,
            LocalTime end,
            String quality,
            String reason
    ) {}

    private final ObservationScoreService observationScoreService;

    public BestWindowService(ObservationScoreService observationScoreService) {
        this.observationScoreService = observationScoreService;
    }

    public List<BestWindow> computeBestWindows(double lat, double lng, LocalDate date) {
        // Currently we only use date (moon phase) and a fixed nighttime window.
        ObservationScoreService.AstronomyData astronomy = observationScoreService.astronomyForDate(date);
        double phase = astronomy.moonPhase();

        LocalTime eveningStart = LocalTime.of(21, 0);
        LocalTime lateEvening = LocalTime.of(23, 0);
        LocalTime aroundMidnightStart = LocalTime.of(23, 0);
        LocalTime aroundMidnightEnd = LocalTime.of(1, 0);
        LocalTime earlyMorningStart = LocalTime.of(1, 0);
        LocalTime earlyMorningEnd = LocalTime.of(3, 0);

        List<BestWindow> windows = new ArrayList<>();

        if (phase < 0.25 || phase > 0.9) {
            windows.add(new BestWindow(
                    eveningStart,
                    earlyMorningEnd,
                    "Excellent",
                    "Near new moon or very thin crescent; sky is dark for most of the night."
            ));
        } else if (phase < 0.5) {
            windows.add(new BestWindow(
                    eveningStart,
                    lateEvening,
                    "Good",
                    "Moon is present but not too bright; earlier evening is better for faint targets."
            ));
            windows.add(new BestWindow(
                    earlyMorningStart,
                    earlyMorningEnd,
                    "Good",
                    "Late night hours when the Moon is lower or has set are better for deep-sky observing."
            ));
        } else if (phase < 0.75) {
            windows.add(new BestWindow(
                    aroundMidnightStart,
                    aroundMidnightEnd,
                    "Fair",
                    "Gibbous Moon; consider shorter sessions around local midnight or focus on brighter targets."
            ));
        } else {
            windows.add(new BestWindow(
                    aroundMidnightStart,
                    aroundMidnightEnd,
                    "Poor",
                    "Near full Moon; best to focus on planets, the Moon itself, or narrowband imaging."
            ));
        }

        return windows;
    }
}

