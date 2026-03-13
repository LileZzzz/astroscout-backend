package com.astroscout.backend.sky;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Sky101Service {

    public Sky101SceneResponse getScene() {
    return new Sky101SceneResponse(List.of(
        star("sun", "Sun", "The star at the center of our solar system.", "The Sun powers Earth's climate and holds the planets in orbit through gravity.",
            facts("149.6 million km", "1.39 million km", "333,000 Earth masses", 0, "Light from the Sun reaches Earth in about 8 minutes 20 seconds."),
            0, 0, 0, 4.8, "#ffd768", "#ff9b2f"),
        planet("mercury", "Mercury", "Closest planet to the Sun.", "Mercury has cratered terrain and the fastest orbit in the solar system.",
            facts("77-222 million km", "4,879 km", "0.055 Earth masses", 0, "A solar day on Mercury lasts longer than its year."),
            10, 0.022, 0.1, 0.72, "#c7bbb0", "#7f756d"),
        planet("venus", "Venus", "Cloud-covered world and Earth's near twin in size.", "Venus has a dense atmosphere and extreme surface temperatures.",
            facts("38-261 million km", "12,104 km", "0.815 Earth masses", 0, "Venus spins backward compared with most planets."),
            14, 0.017, 1.2, 1.05, "#f1d4a5", "#ba9155"),
        planet("earth", "Earth", "Our home planet.", "Earth is the only known world with life and abundant liquid surface water.",
            facts("0 km", "12,742 km", "1 Earth mass", 1, "About 71% of Earth's surface is covered by water."),
            18, 0.014, 2.0, 1.1, "#72b6ff", "#2f7cd5"),
        planet("mars", "Mars", "The red planet.", "Mars has polar caps, giant volcanoes, and strong evidence of ancient rivers and lakes.",
            facts("54.6-401 million km", "6,779 km", "0.107 Earth masses", 2, "Olympus Mons is the tallest volcano known in the solar system."),
            23, 0.011, 2.8, 0.86, "#d96c4e", "#7b3523"),
        planet("jupiter", "Jupiter", "Largest planet in the solar system.", "Jupiter is a gas giant with cloud bands and the Great Red Spot storm.",
            facts("588-968 million km", "139,820 km", "317.8 Earth masses", 95, "Jupiter's magnetic field is the strongest of any planet."),
            31, 0.007, 1.6, 2.3, "#e2b56a", "#a16c33"),
        ringedPlanet("saturn", "Saturn", "Ringed giant planet.", "Saturn's rings are made from countless ice-rich particles orbiting the planet.",
            facts("1.2-1.7 billion km", "116,460 km", "95.2 Earth masses", 146, "Saturn would float in a giant enough ocean because its average density is lower than water."),
            40, 0.0055, 0.8, 1.9, "#d7c49c", "#b08f56"),
        planet("uranus", "Uranus", "Ice giant tilted on its side.", "Uranus rotates almost sideways relative to its orbital plane.",
            facts("2.6-3.2 billion km", "50,724 km", "14.5 Earth masses", 28, "Uranus may have been knocked onto its side by a giant impact."),
            49, 0.0045, 1.9, 1.55, "#92f1f3", "#56bcc5"),
        planet("neptune", "Neptune", "Distant blue ice giant.", "Neptune has strong winds and a deep blue atmosphere rich in methane.",
            facts("4.3-4.7 billion km", "49,244 km", "17.1 Earth masses", 16, "Neptune was discovered partly from math before it was seen in a telescope."),
            57, 0.0038, 2.7, 1.48, "#90a9ff", "#5d73d9"),

        galaxy("milky-way", "Milky Way", "Our home galaxy.", "The Milky Way is a barred spiral galaxy containing the Sun and hundreds of billions of stars.",
            facts("26,670 light-years to the galactic center", "100,000-120,000 light-years", "about 1-1.5 trillion solar masses", null, "The Sun takes about 230 million years to orbit the Milky Way once."),
            -66, 24, -8, 4.8, "#ffe6b3", "#ffb15e"),
        galaxy("andromeda", "Andromeda Galaxy", "Nearest major spiral galaxy.", "Andromeda is about 2.5 million light-years away and visible in dark skies without a telescope.",
            facts("2.5 million light-years", "about 220,000 light-years", "about 1.2 trillion solar masses", null, "Andromeda and the Milky Way are expected to merge in about 4.5 billion years."),
            66, -12, 2, 4.0, "#cfd6ff", "#8da2ff"),
        galaxy("triangulum", "Triangulum Galaxy", "A smaller spiral in the Local Group.", "Triangulum is a diffuse galaxy often observed from dark sites.",
            facts("2.73 million light-years", "about 60,000 light-years", "about 50 billion solar masses", null, "Triangulum is also cataloged as M33."),
            58, 18, 14, 3.0, "#9ed8ff", "#58a8ff"),
        galaxy("orion-nebula", "Orion Nebula", "A bright stellar nursery in Orion.", "The Orion Nebula is one of the best beginner deep-sky telescope targets.",
            facts("1,344 light-years", "about 24 light-years", "about 2,000 solar masses", null, "Newborn stars inside the nebula are less than a few million years old."),
            -54, -18, 20, 3.0, "#a9e5ff", "#6ab4ff"),

        constellation("aries", "Aries", "Zodiac constellation of the ram.", "Aries sits along the ecliptic and is often used to introduce zodiac constellations.",
            facts("about 66-164 light-years for main stars", "Sky pattern spans about 20 degrees", null, null, "Hamal, the brightest star in Aries, is an orange giant."),
            -50, 34, 34,
            pt(-5, -2, 0, 0.32, "#ffe49f"), pt(0, 2, 0, 0.38, "#fff2cc"), pt(5, 5, 0, 0.3, "#ffe49f"),
            List.of(link(0, 1), link(1, 2))),
        constellation("taurus", "Taurus", "Bull constellation with Aldebaran.", "Taurus is associated with the Hyades and the nearby Pleiades star cluster.",
            facts("about 65 light-years to the Hyades core", "Sky pattern spans about 30 degrees", null, null, "Taurus contains both the Hyades and the Pleiades."),
            -38, 38, 30,
            pt(-6, 0, 0, 0.32, "#ffe49f"), pt(-1, 3, 0, 0.42, "#ffd978"), pt(4, -1, 0, 0.28, "#ffe49f"), pt(8, 4, 0, 0.26, "#fff2cc"),
            List.of(link(0, 1), link(1, 2), link(1, 3))),
        constellation("gemini", "Gemini", "The twins Castor and Pollux.", "Gemini is easy to spot because two bright stars anchor the pattern.",
            facts("about 34-52 light-years for Castor and Pollux", "Sky pattern spans about 30 degrees", null, null, "Pollux hosts at least one confirmed exoplanet."),
            -20, 40, 26,
            pt(-4, 4, 0, 0.34, "#fff2cc"), pt(3, 3, 0, 0.34, "#fff2cc"), pt(-2, -2, 0, 0.26, "#ffe49f"), pt(5, -3, 0, 0.26, "#ffe49f"),
            List.of(link(0, 2), link(1, 3), link(2, 3))),
        constellation("cancer", "Cancer", "Faint zodiac constellation.", "Cancer contains the Beehive Cluster, a classic binocular target.",
            facts("577 light-years to the Beehive Cluster", "Sky pattern spans about 20 degrees", null, null, "The Beehive Cluster is visible to the naked eye in dark skies."),
            -2, 42, 24,
            pt(-4, 0, 0, 0.22, "#ffe49f"), pt(0, 4, 0, 0.28, "#fff2cc"), pt(4, 0, 0, 0.22, "#ffe49f"), pt(0, -4, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3), link(3, 0))),
        constellation("leo", "Leo", "The lion marked by Regulus.", "Leo's sickle-shaped head makes it one of the more recognizable zodiac constellations.",
            facts("79 light-years to Regulus", "Sky pattern spans about 30 degrees", null, null, "Leo's sickle asterism looks like a backward question mark."),
            16, 40, 20,
            pt(-6, 2, 0, 0.3, "#ffe49f"), pt(-2, 5, 0, 0.42, "#fff2cc"), pt(2, 2, 0, 0.28, "#ffe49f"), pt(6, -1, 0, 0.26, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3))),
        constellation("virgo", "Virgo", "Large zodiac constellation with Spica.", "Virgo appears in spring skies and points toward a rich galaxy field.",
            facts("250 light-years to Spica", "Sky pattern spans about 50 degrees", null, null, "Virgo points toward the Virgo Cluster, a massive galaxy collection."),
            34, 36, 16,
            pt(-5, 4, 0, 0.24, "#ffe49f"), pt(0, 0, 0, 0.42, "#fff2cc"), pt(5, -3, 0, 0.24, "#ffe49f"), pt(8, 3, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(1, 3))),
        constellation("libra", "Libra", "The scales.", "Libra is one of the few zodiac constellations representing an object instead of a creature.",
            facts("77 light-years to Zubenelgenubi", "Sky pattern spans about 23 degrees", null, null, "Libra used to be associated with the claws of Scorpius."),
            50, 30, 12,
            pt(-4, 3, 0, 0.25, "#ffe49f"), pt(0, 0, 0, 0.3, "#fff2cc"), pt(4, 3, 0, 0.25, "#ffe49f"), pt(0, -4, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(1, 3))),
        constellation("scorpius", "Scorpius", "The scorpion with bright Antares.", "Scorpius traces a hook-like body and tail low in many summer skies.",
            facts("550 light-years to Antares", "Sky pattern spans about 35 degrees", null, null, "Antares is a red supergiant that would engulf Mars if placed where the Sun is."),
            56, 14, 10,
            pt(-6, 2, 0, 0.24, "#ffe49f"), pt(-2, 0, 0, 0.42, "#ffb075"), pt(2, -3, 0, 0.28, "#ffe49f"), pt(6, -6, 0, 0.24, "#ffe49f"), pt(10, -3, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3), link(3, 4))),
        constellation("sagittarius", "Sagittarius", "Teapot-shaped zodiac constellation.", "Sagittarius points toward the center of the Milky Way and is rich with nebulae.",
            facts("26,670 light-years to the Milky Way center direction", "Sky pattern spans about 36 degrees", null, null, "Looking toward Sagittarius means looking toward our galaxy's center."),
            52, 0, 12,
            pt(-6, 2, 0, 0.24, "#ffe49f"), pt(-2, 5, 0, 0.24, "#ffe49f"), pt(2, 4, 0, 0.24, "#ffe49f"), pt(6, 0, 0, 0.24, "#ffe49f"), pt(0, -4, 0, 0.24, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3), link(2, 4))),
        constellation("capricornus", "Capricornus", "The sea-goat.", "Capricornus is compact and often viewed low in the southern sky.",
            facts("39 light-years to Deneb Algedi", "Sky pattern spans about 23 degrees", null, null, "Capricornus is one of the oldest constellations on record."),
            38, -10, 14,
            pt(-5, 2, 0, 0.23, "#ffe49f"), pt(-1, -2, 0, 0.26, "#fff2cc"), pt(4, 2, 0, 0.23, "#ffe49f"), pt(8, -3, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3))),
        constellation("aquarius", "Aquarius", "The water bearer.", "Aquarius is broad and faint, making it best appreciated under darker skies.",
            facts("92 light-years to Sadalsuud", "Sky pattern spans about 40 degrees", null, null, "Aquarius hosts the Helix Nebula, a favorite astrophotography target."),
            18, -18, 18,
            pt(-6, 2, 0, 0.22, "#ffe49f"), pt(-2, 4, 0, 0.24, "#fff2cc"), pt(3, 1, 0, 0.22, "#ffe49f"), pt(7, -3, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3))),
        constellation("pisces", "Pisces", "The fishes.", "Pisces stretches across a large region and marks a zodiac sector of the night sky.",
            facts("about 130 light-years to Alrescha", "Sky pattern spans about 50 degrees", null, null, "The spring equinox currently lies in Pisces because of Earth's axial precession."),
            -2, -22, 20,
            pt(-6, 2, 0, 0.22, "#ffe49f"), pt(-2, -1, 0, 0.24, "#fff2cc"), pt(3, 2, 0, 0.22, "#ffe49f"), pt(8, -2, 0, 0.22, "#ffe49f"),
            List.of(link(0, 1), link(1, 2), link(2, 3))),
        constellation("orion", "Orion", "One of the easiest constellations to recognize.", "Orion contains Betelgeuse, Rigel, and the famous three-star belt near the Orion Nebula.",
            facts("642.5 light-years to the Orion Nebula", "Sky pattern spans about 25 degrees", null, null, "Betelgeuse is a variable red supergiant nearing the end of its life."),
            -30, -16, 18,
            pt(-6, 6, 0, 0.4, "#ffbf90"), pt(5, 7, 0, 0.34, "#c9ddff"), pt(-3, 1, 0, 0.22, "#fff2cc"), pt(0, 0, 0, 0.24, "#fff2cc"), pt(3, -1, 0, 0.22, "#fff2cc"), pt(-5, -7, 0, 0.3, "#c9ddff"), pt(6, -8, 0, 0.32, "#b0c8ff"),
            List.of(link(0, 2), link(2, 3), link(3, 4), link(4, 1), link(2, 5), link(4, 6)))
    ));
    }

    private Sky101Object star(String id, String name, String shortDescription, String details,
                  Sky101FactSheet facts,
                  double x, double y, double z, double radius, String color, String accent) {
    return new Sky101Object(id, name, "PLANET", "SUN", shortDescription, details, facts, x, y, z, radius,
        color, accent, null, null, null, List.of(), List.of());
    }

    private Sky101Object planet(String id, String name, String shortDescription, String details,
                Sky101FactSheet facts,
                double orbitRadius, double orbitSpeed, double orbitPhase,
                double radius, String color, String accent) {
    return new Sky101Object(id, name, "PLANET", "PLANET", shortDescription, details, facts, 0, 0, 0, radius,
        color, accent, orbitRadius, orbitSpeed, orbitPhase, List.of(), List.of());
    }

    private Sky101Object ringedPlanet(String id, String name, String shortDescription, String details,
                      Sky101FactSheet facts,
                      double orbitRadius, double orbitSpeed, double orbitPhase,
                      double radius, String color, String accent) {
    return new Sky101Object(id, name, "PLANET", "RINGED_PLANET", shortDescription, details, facts, 0, 0, 0, radius,
        color, accent, orbitRadius, orbitSpeed, orbitPhase, List.of(), List.of());
    }

    private Sky101Object galaxy(String id, String name, String shortDescription, String details,
                Sky101FactSheet facts,
                double x, double y, double z, double radius, String color, String accent) {
    return new Sky101Object(id, name, "GALAXY", "GALAXY", shortDescription, details, facts, x, y, z, radius,
        color, accent, null, null, null, List.of(), List.of());
    }

    private Sky101Object constellation(String id, String name, String shortDescription, String details,
                       Sky101FactSheet facts,
                       double x, double y, double z,
                       Sky101ShapePoint p1, Sky101ShapePoint p2, Sky101ShapePoint p3,
                       List<Sky101ShapeLink> links) {
    return new Sky101Object(id, name, "CONSTELLATION", "CONSTELLATION", shortDescription, details, facts, x, y, z,
        2.8, "#ffe49f", "#fff7dc", null, null, null, List.of(p1, p2, p3), links);
    }

    private Sky101Object constellation(String id, String name, String shortDescription, String details,
                       Sky101FactSheet facts,
                       double x, double y, double z,
                       Sky101ShapePoint p1, Sky101ShapePoint p2, Sky101ShapePoint p3, Sky101ShapePoint p4,
                       List<Sky101ShapeLink> links) {
    return new Sky101Object(id, name, "CONSTELLATION", "CONSTELLATION", shortDescription, details, facts, x, y, z,
        3.1, "#ffe49f", "#fff7dc", null, null, null, List.of(p1, p2, p3, p4), links);
    }

    private Sky101Object constellation(String id, String name, String shortDescription, String details,
                       Sky101FactSheet facts,
                       double x, double y, double z,
                       Sky101ShapePoint p1, Sky101ShapePoint p2, Sky101ShapePoint p3, Sky101ShapePoint p4, Sky101ShapePoint p5,
                       List<Sky101ShapeLink> links) {
    return new Sky101Object(id, name, "CONSTELLATION", "CONSTELLATION", shortDescription, details, facts, x, y, z,
        3.4, "#ffe49f", "#fff7dc", null, null, null, List.of(p1, p2, p3, p4, p5), links);
    }

    private Sky101Object constellation(String id, String name, String shortDescription, String details,
                       Sky101FactSheet facts,
                       double x, double y, double z,
                       Sky101ShapePoint p1, Sky101ShapePoint p2, Sky101ShapePoint p3, Sky101ShapePoint p4,
                       Sky101ShapePoint p5, Sky101ShapePoint p6, Sky101ShapePoint p7,
                       List<Sky101ShapeLink> links) {
    return new Sky101Object(id, name, "CONSTELLATION", "CONSTELLATION", shortDescription, details, facts, x, y, z,
        4.4, "#ffe49f", "#fff7dc", null, null, null, List.of(p1, p2, p3, p4, p5, p6, p7), links);
    }

    private Sky101FactSheet facts(String distanceFromEarth, String diameter, String mass,
                      Integer moonCount, String funFact) {
    return new Sky101FactSheet(distanceFromEarth, diameter, mass, moonCount, funFact);
    }

    private Sky101ShapePoint pt(double x, double y, double z, double size, String color) {
    return new Sky101ShapePoint(x, y, z, size, color);
    }

    private Sky101ShapeLink link(int from, int to) {
    return new Sky101ShapeLink(from, to, "#4f6ea7");
    }
}
