package com.astroscout.backend.observation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class OpenMeteoWeatherService {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final double GRID_DEGREES = 0.25;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate redisTemplate;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer fetchTimer;

    public OpenMeteoWeatherService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.cacheHitCounter = Counter.builder("astroscout.weather.cache.hit").register(meterRegistry);
        this.cacheMissCounter = Counter.builder("astroscout.weather.cache.miss").register(meterRegistry);
        this.fetchTimer = Timer.builder("astroscout.weather.fetch").register(meterRegistry);
    }

    public ObservationScoreService.WeatherData getWeather(double lat, double lng, LocalDate date) {
        ObservationScoreService.WeatherData cached = getFromCache(lat, lng, date);
        if (cached != null) {
            cacheHitCounter.increment();
            return cached;
        }
        cacheMissCounter.increment();

        return fetchTimer.record(() -> {
            try {
            String params = buildQuery(lat, lng, date);
            URI uri = new URI(BASE_URL + "?" + params);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new ObservationScoreService.WeatherData(50, 10.0, 50, 3.0);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode hourly = root.path("hourly");
            JsonNode times = hourly.path("time");
            JsonNode cloudcover = hourly.path("cloudcover");
            JsonNode visibility = hourly.path("visibility");
            JsonNode humidity = hourly.path("relativehumidity_2m");
            JsonNode windspeed = hourly.path("windspeed_10m");

            if (!times.isArray() || times.isEmpty()) {
                return new ObservationScoreService.WeatherData(50, 10.0, 50, 3.0);
            }

            LocalDateTime target = date.atTime(22, 0);
            int bestIndex = 0;
            long bestDiff = Long.MAX_VALUE;

            for (int i = 0; i < times.size(); i++) {
                String timeStr = times.get(i).asText();
                LocalDateTime t = LocalDateTime.parse(timeStr);
                long diff = Math.abs(t.toEpochSecond(ZoneOffset.UTC) - target.toEpochSecond(ZoneOffset.UTC));
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestIndex = i;
                }
            }

            int cloudPercent = cloudcover.path(bestIndex).asInt(50);
            double visibilityMeters = visibility.path(bestIndex).asDouble(10000.0);
            int humidityPercent = humidity.path(bestIndex).asInt(50);
            double windKmh = windspeed.path(bestIndex).asDouble(10.0);

            double visibilityKm = visibilityMeters / 1000.0;
            double windMpsRaw = windKmh / 3.6;
            double windMps = Math.round(windMpsRaw * 100.0) / 100.0;

            ObservationScoreService.WeatherData result = new ObservationScoreService.WeatherData(
                    cloudPercent,
                    visibilityKm,
                    humidityPercent,
                    windMps
            );
            putIntoCache(lat, lng, date, result);
            return result;
            } catch (Exception e) {
                return new ObservationScoreService.WeatherData(50, 10.0, 50, 3.0);
            }
        });
    }

    private String buildQuery(double lat, double lng, LocalDate date) {
        String encodedParams =
                "latitude=" + URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8) +
                        "&longitude=" + URLEncoder.encode(String.valueOf(lng), StandardCharsets.UTF_8) +
                        "&hourly=cloudcover,visibility,relativehumidity_2m,windspeed_10m" +
                        "&timezone=UTC" +
                        "&start_date=" + date +
                        "&end_date=" + date;
        return encodedParams;
    }

    private String buildCacheKey(double lat, double lng, LocalDate date) {
        double roundedLat = Math.round(lat / GRID_DEGREES) * GRID_DEGREES;
        double roundedLng = Math.round(lng / GRID_DEGREES) * GRID_DEGREES;
        return "weather:" + roundedLat + ":" + roundedLng + ":" + date;
    }

    private ObservationScoreService.WeatherData getFromCache(double lat, double lng, LocalDate date) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String key = buildCacheKey(lat, lng, date);
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            String[] parts = raw.split(",");
            if (parts.length != 4) {
                return null;
            }
            int cloud = Integer.parseInt(parts[0]);
            double visibilityKm = Double.parseDouble(parts[1]);
            int humidity = Integer.parseInt(parts[2]);
            double wind = Double.parseDouble(parts[3]);
            return new ObservationScoreService.WeatherData(cloud, visibilityKm, humidity, wind);
        } catch (DataAccessException | NumberFormatException ex) {
            return null;
        }
    }

    private void putIntoCache(double lat, double lng, LocalDate date, ObservationScoreService.WeatherData data) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String key = buildCacheKey(lat, lng, date);
            String raw = data.cloudCoverPercent() + "," +
                    data.visibilityKm() + "," +
                    data.humidityPercent() + "," +
                    data.windSpeedMps();
            redisTemplate.opsForValue().set(key, raw, java.time.Duration.ofMinutes(15));
        } catch (DataAccessException ignored) {
        }
    }
}

