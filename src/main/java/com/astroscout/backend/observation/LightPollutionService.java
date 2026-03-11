package com.astroscout.backend.observation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LightPollutionService {

    private static final Duration LIGHT_POLLUTION_TTL = Duration.ofDays(30);
    private static final double GRID_DEGREES = 0.25;

    private final StringRedisTemplate redisTemplate;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer fetchTimer;

    public LightPollutionService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.cacheHitCounter = Counter.builder("astroscout.light.cache.hit").register(meterRegistry);
        this.cacheMissCounter = Counter.builder("astroscout.light.cache.miss").register(meterRegistry);
        this.fetchTimer = Timer.builder("astroscout.light.fetch").register(meterRegistry);
    }

    public ObservationScoreService.LightPollutionData getLightPollution(double lat, double lng) {
        Integer bortle = getFromCache(lat, lng);
        if (bortle == null) {
            cacheMissCounter.increment();
            bortle = fetchTimer.record(() -> fetchFromSource(lat, lng));
            putIntoCache(lat, lng, bortle);
        } else {
            cacheHitCounter.increment();
        }
        return new ObservationScoreService.LightPollutionData(bortle);
    }

    private String buildKey(double lat, double lng) {
        double roundedLat = Math.round(lat / GRID_DEGREES) * GRID_DEGREES;
        double roundedLng = Math.round(lng / GRID_DEGREES) * GRID_DEGREES;
        return "light:" + roundedLat + ":" + roundedLng;
    }

    private Integer getFromCache(double lat, double lng) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String key = buildKey(lat, lng);
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private void putIntoCache(double lat, double lng, int bortle) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String key = buildKey(lat, lng);
            redisTemplate.opsForValue().set(key, Integer.toString(bortle), LIGHT_POLLUTION_TTL);
        } catch (DataAccessException ignored) {
        }
    }

    private int fetchFromSource(double lat, double lng) {
        double absLat = Math.abs(lat);
        int base;
        if (absLat < 5) {
            base = 5;
        } else if (absLat < 25) {
            base = 4;
        } else if (absLat < 45) {
            base = 3;
        } else if (absLat < 60) {
            base = 4;
        } else {
            base = 5;
        }

        if (Math.abs(lat - 40.7128) < 0.5 && Math.abs(lng + 74.0060) < 0.5) {
            return 8;
        }
        if (Math.abs(lat - 34.0522) < 0.5 && Math.abs(lng + 118.2437) < 0.5) {
            return 8;
        }
        if (Math.abs(lat - 51.5074) < 0.5 && Math.abs(lng + 0.1278) < 0.5) {
            return 8;
        }
        if (Math.abs(lat - 35.6895) < 0.5 && Math.abs(lng - 139.6917) < 0.5) {
            return 9;
        }

        int clamped = Math.min(9, Math.max(1, base));
        return clamped;
    }
}

