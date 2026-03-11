package com.astroscout.backend.observation;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CelestialCatalogService {

    public record CelestialObject(
            String name,
            String type,
            double raHours,      // Right ascension in hours (0-24)
            double decDeg,       // Declination in degrees (-90 to 90)
            double magnitude,
            boolean needsTelescope,
            String description
    ) {}

    public record VisibleObject(
            String name,
            String type,
            double altDeg,
            double azDeg,
            double magnitude,
            boolean needsTelescope,
            String description
    ) {}

    private final List<CelestialObject> catalog = List.of(
            new CelestialObject(
                    "Jupiter",
                    "PLANET",
                    0.0,
                    0.0,
                    -2.5,
                    false,
                    "Bright gas giant, easy visual target and great for planetary imaging."
            ),
            new CelestialObject(
                    "Saturn",
                    "PLANET",
                    21.0,
                    -16.0,
                    0.7,
                    false,
                    "Ringed gas giant; stunning visual target at medium magnification."
            ),
            new CelestialObject(
                    "Mars",
                    "PLANET",
                    5.0,
                    25.0,
                    0.5,
                    false,
                    "Red planet; small disk but high-contrast surface features during oppositions."
            ),
            new CelestialObject(
                    "Venus",
                    "PLANET",
                    10.0,
                    10.0,
                    -4.0,
                    false,
                    "Extremely bright inner planet, best seen during twilight as a brilliant crescent."
            ),
            new CelestialObject(
                    "Orion Nebula (M42)",
                    "DSO",
                    5.591,
                    -5.45,
                    4.0,
                    true,
                    "Iconic emission nebula in Orion, ideal for wide-field astrophotography."
            ),
            new CelestialObject(
                    "Pleiades (M45)",
                    "OPEN_CLUSTER",
                    3.791,
                    24.12,
                    1.6,
                    false,
                    "Bright open cluster; excellent binocular and wide‑field imaging target."
            ),
            new CelestialObject(
                    "Andromeda Galaxy (M31)",
                    "GALAXY",
                    0.712,
                    41.27,
                    3.4,
                    true,
                    "Nearest major galaxy; large, extended target that benefits from dark skies."
            ),
            new CelestialObject(
                    "Lagoon Nebula (M8)",
                    "DSO",
                    18.068,
                    -24.38,
                    6.0,
                    true,
                    "Bright emission nebula in Sagittarius; great wide-field summer Milky Way target."
            ),
            new CelestialObject(
                    "Trifid Nebula (M20)",
                    "DSO",
                    18.071,
                    -23.02,
                    6.3,
                    true,
                    "Compact nebula with both emission and reflection components near the Lagoon Nebula."
            ),
            new CelestialObject(
                    "North America Nebula (NGC 7000)",
                    "DSO",
                    20.967,
                    44.32,
                    7.0,
                    true,
                    "Large, faint nebula in Cygnus resembling the outline of the North American continent."
            ),
            new CelestialObject(
                    "Double Cluster (NGC 869/884)",
                    "OPEN_CLUSTER",
                    2.333,
                    57.15,
                    4.3,
                    false,
                    "Two rich open clusters in Perseus; spectacular in wide-field eyepieces or binoculars."
            ),
            new CelestialObject(
                    "Sirius",
                    "STAR",
                    6.752,
                    -16.72,
                    -1.46,
                    false,
                    "Brightest star in the night sky, located in Canis Major."
            ),
            new CelestialObject(
                    "Vega",
                    "STAR",
                    18.615,
                    38.78,
                    0.03,
                    false,
                    "Very bright blue-white star in Lyra; forms part of the Summer Triangle."
            ),
            new CelestialObject(
                    "Deneb",
                    "STAR",
                    20.691,
                    45.28,
                    1.25,
                    false,
                    "Luminous supergiant in Cygnus; another corner of the Summer Triangle."
            ),
            new CelestialObject(
                    "Altair",
                    "STAR",
                    19.846,
                    8.87,
                    0.77,
                    false,
                    "Fast-rotating bright star in Aquila; completes the Summer Triangle asterism."
            )
    );

    public List<VisibleObject> computeVisible(double latDeg, double lngDeg, LocalDate date, LocalTime time) {
        LocalTime observingTime = time != null ? time : LocalTime.of(22, 0);
        LocalDateTime localDateTime = LocalDateTime.of(date, observingTime);
        ZonedDateTime utc = localDateTime.atZone(ZoneOffset.UTC);

        double lstHours = localSiderealTimeHours(utc, lngDeg);

        double latRad = Math.toRadians(latDeg);

        List<VisibleObject> visible = new ArrayList<>();
        for (CelestialObject obj : catalog) {
            double haHours = lstHours - obj.raHours;
            haHours = normalizeHours(haHours);
            double haRad = Math.toRadians(haHours * 15.0);
            double decRad = Math.toRadians(obj.decDeg);

            double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                    + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
            double altRad = Math.asin(clamp(sinAlt, -1.0, 1.0));

            double cosAz = (Math.sin(decRad) - Math.sin(altRad) * Math.sin(latRad))
                    / (Math.cos(altRad) * Math.cos(latRad) + 1e-10);
            cosAz = clamp(cosAz, -1.0, 1.0);
            double azRad = Math.acos(cosAz);

            if (Math.sin(haRad) > 0) {
                azRad = 2 * Math.PI - azRad;
            }

            double altDeg = Math.toDegrees(altRad);
            double azDeg = Math.toDegrees(azRad);

            if (altDeg < 10.0) {
                continue;
            }

            visible.add(new VisibleObject(
                    obj.name,
                    obj.type,
                    altDeg,
                    azDeg,
                    obj.magnitude,
                    obj.needsTelescope,
                    obj.description
            ));
        }

        visible.sort((a, b) -> Double.compare(b.altDeg, a.altDeg));

        return visible;
    }

    private static double localSiderealTimeHours(ZonedDateTime utc, double lngDeg) {
        ZonedDateTime j2000 = ZonedDateTime.of(
                2000, 1, 1,
                12, 0, 0, 0,
                ZoneOffset.UTC
        );
        double days = (utc.toInstant().toEpochMilli() - j2000.toInstant().toEpochMilli())
                / (1000.0 * 60.0 * 60.0 * 24.0);

        double gmst = 18.697374558 + 24.06570982441908 * days;
        double lst = gmst + lngDeg / 15.0;
        return normalizeHours(lst);
    }

    private static double normalizeHours(double hours) {
        double h = hours % 24.0;
        if (h < 0) {
            h += 24.0;
        }
        return h;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

