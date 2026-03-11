package com.astroscout.backend.seed;

import com.astroscout.backend.observation.Comment;
import com.astroscout.backend.observation.CommentRepository;
import com.astroscout.backend.observation.LogLike;
import com.astroscout.backend.observation.LogLikeRepository;
import com.astroscout.backend.observation.ObservationLog;
import com.astroscout.backend.observation.ObservationLogRepository;
import com.astroscout.backend.user.User;
import com.astroscout.backend.user.UserRepository;
import com.astroscout.backend.user.User.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ObservationLogRepository observationLogRepository;
    private final LogLikeRepository logLikeRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String SEED_PASSWORD = "password";

    public DataSeeder(
            UserRepository userRepository,
            ObservationLogRepository observationLogRepository,
            LogLikeRepository logLikeRepository,
            CommentRepository commentRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.observationLogRepository = observationLogRepository;
        this.logLikeRepository = logLikeRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        truncateAllTables();
        List<User> users = seedUsers();
        List<ObservationLog> logs = seedLogs(users);
        seedLikesAndComments(users, logs);
    }

    /**
     * Truncate all seed-relevant tables and reset their sequences in one go.
     * TRUNCATE ... RESTART IDENTITY CASCADE avoids deleteAll() transaction issues
     * and ensures IDs start from 1 after reseed.
     */
    private void truncateAllTables() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE users, observation_logs, log_likes, comments RESTART IDENTITY CASCADE"
        );
    }

    private List<User> seedUsers() {
        String hash = passwordEncoder.encode(SEED_PASSWORD);
        List<User> users = List.of(
                new User("celeste.vega@example.com", hash, "CelesteVega"),
                new User("orion.nebula@example.com", hash, "OrionNebula"),
                new User("luna.stargazer@example.com", hash, "LunaStargazer"),
                new User("sirius.observer@example.com", hash, "SiriusObserver"),
                new User("andromeda.sky@example.com", hash, "AndromedaSky"),
                new User("admin@astroscout.example.com", hash, "AstroAdmin")
        );
        users = userRepository.saveAll(users);
        User admin = users.get(users.size() - 1);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return users;
    }

    private List<ObservationLog> seedLogs(List<User> users) {
        Instant base = Instant.now().minus(14, ChronoUnit.DAYS);
        User u1 = users.get(0);
        User u2 = users.get(1);
        User u3 = users.get(2);
        User u4 = users.get(3);
        User u5 = users.get(4);

        List<ObservationLog> logs = new java.util.ArrayList<>();

        // Original 8 logs
        logs.add(createLog(u1, "Jupiter and Galilean Moons at 8-inch Dob",
                "Clear view of Io, Europa, Ganymede, and Callisto. Great seeing, Bortle 4. Jupiter's bands very distinct.",
                base.plus(12, ChronoUnit.DAYS), "Blue Ridge Overlook, VA", 37.5, -79.0, 4, "Clear", 8, true));
        logs.add(createLog(u2, "First light: M31 Andromeda Galaxy",
                "First session with the new 10-inch. Andromeda core and dust lanes visible. M32 and M110 in same field.",
                base.plus(10, ChronoUnit.DAYS), "Cherry Springs State Park, PA", 41.66, -77.82, 2, "Clear", 9, true));
        logs.add(createLog(u3, "Perseid meteor shower peak",
                "Counted 87 meteors in 90 minutes. Several fireballs. Milky Way overhead.",
                base.plus(8, ChronoUnit.DAYS), "Death Valley National Park", 36.5, -117.0, 1, "Clear", 10, true));
        logs.add(createLog(u4, "Lunar eclipse totality",
                "Full totality at 3:47 AM. Copper-red disk, no scope needed. Neighbors joined in the driveway.",
                base.plus(5, ChronoUnit.DAYS), "Backyard, suburban Bortle 6", 40.7, -74.0, 6, "Clear", 7, true));
        logs.add(createLog(u5, "M42 Orion Nebula through 6-inch refractor",
                "Trapezium resolved, nebulosity extended. Used O-III filter for outer regions.",
                base.plus(3, ChronoUnit.DAYS), "Dark Sky Campground, TX", 30.5, -99.2, 3, "Clear", 8, true));
        logs.add(createLog(u1, "Saturn ring tilt and Titan",
                "Rings at good angle. Titan visible as dot. Seeing 7/10.",
                base.plus(1, ChronoUnit.DAYS), "Rural backyard, OH", 41.2, -81.5, 4, "Stable", 7, true));
        logs.add(createLog(u2, "Double Cluster in Perseus",
                "Both NGC 869 and 884 in same low-power field. Stunning in 20x80 binos.",
                base, "Adirondack Dark Sky Site", 44.0, -74.0, 3, "Clear", 8, true));
        logs.add(createLog(u3, "Private test log", "Not for feed.",
                base.plus(6, ChronoUnit.DAYS), "Home", 42.0, -71.0, 7, "Cloudy", 4, false));

        // 22 more logs (30 total, 29 public)
        logs.add(createLog(u4, "Venus and Mercury at dawn",
                "Pair nicely in the east. Mercury harder to spot but both clear in 10x50.",
                base.plus(11, ChronoUnit.DAYS), "Lakeshore, MI", 42.3, -83.0, 5, "Clear", 7, true));
        logs.add(createLog(u5, "M13 Hercules Cluster",
                "Resolved to the core with 8-inch. Beautiful globular.",
                base.plus(9, ChronoUnit.DAYS), "Big Bend, TX", 29.3, -103.2, 1, "Clear", 9, true));
        logs.add(createLog(u1, "ISS pass and Starlink train",
                "Bright pass at -3 mag, then Starlink chain 10 min later. Kids loved it.",
                base.plus(7, ChronoUnit.DAYS), "Denver suburbs", 39.7, -105.0, 6, "Clear", 6, true));
        logs.add(createLog(u2, "Pleiades and Hyades",
                "M45 and Hyades in same binos field. Great for wide-field photography.",
                base.plus(4, ChronoUnit.DAYS), "Joshua Tree, CA", 34.0, -116.0, 2, "Clear", 8, true));
        logs.add(createLog(u3, "Mars near opposition",
                "Red disk visible at 120x. Syrtis Major and polar cap hinted.",
                base.plus(2, ChronoUnit.DAYS), "Chaco Canyon, NM", 36.0, -108.0, 1, "Clear", 9, true));
        logs.add(createLog(u4, "Milky Way core from 9000 ft",
                "Sagittarius region stunning. Dark site at altitude made a huge difference.",
                base.plus(13, ChronoUnit.DAYS), "Cedar Breaks, UT", 37.6, -112.8, 2, "Clear", 10, true));
        logs.add(createLog(u5, "Comet C/2024 session",
                "Faint but tail visible in 15x70. Tracked for an hour.",
                base.plus(10, ChronoUnit.DAYS), "Mauna Kea visitor area", 19.8, -155.5, 2, "Clear", 9, true));
        logs.add(createLog(u1, "North America Nebula",
                "NGC 7000 in O-III. California clearly defined. 4-inch refractor.",
                base.plus(8, ChronoUnit.DAYS), "Spruce Knob, WV", 38.7, -79.5, 2, "Clear", 8, true));
        logs.add(createLog(u2, "Jupiter GRS transit",
                "Great Red Spot crossed center around 11pm. Good seeing.",
                base.plus(6, ChronoUnit.DAYS), "Backyard, Phoenix", 33.4, -112.1, 7, "Stable", 6, true));
        logs.add(createLog(u3, "Ring Nebula M57",
                "Classic smoke ring. Easy at 50x, detail at 150x.",
                base.plus(5, ChronoUnit.DAYS), "Catskills, NY", 42.2, -74.2, 4, "Clear", 7, true));
        logs.add(createLog(u4, "Omega Nebula M17",
                "Swan shape obvious. H-alpha helped. Bortle 3 site.",
                base.plus(4, ChronoUnit.DAYS), "Black Rock Desert, NV", 40.9, -119.0, 2, "Clear", 8, true));
        logs.add(createLog(u5, "Lunar crater hop",
                "Tycho, Clavius, Plato. Terminator was ideal for shadows.",
                base.plus(3, ChronoUnit.DAYS), "Local park", 34.0, -118.2, 7, "Clear", 5, true));
        logs.add(createLog(u1, "Ursa Major galaxies",
                "M81, M82, NGC 3077 in one field. M82 starburst visible.",
                base.plus(2, ChronoUnit.DAYS), "Boundary Waters, MN", 48.0, -91.0, 1, "Clear", 9, true));
        logs.add(createLog(u2, "Summer Triangle session",
                "Vega, Altair, Deneb. Just naked-eye and binos. Relaxing night.",
                base.plus(1, ChronoUnit.DAYS), "Cape Cod", 41.7, -70.2, 5, "Clear", 7, true));
        logs.add(createLog(u3, "Saturn moons",
                "Titan, Rhea, Dione, Tethys. Could not get Enceladus tonight.",
                base, "Great Basin NP", 38.9, -114.2, 1, "Clear", 9, true));
        logs.add(createLog(u4, "Crab Nebula M1",
                "Faint fuzzy in 6-inch. Needed averted vision. Supernova remnant!",
                base.plus(12, ChronoUnit.DAYS), "Acadia NP", 44.3, -68.2, 3, "Clear", 8, true));
        logs.add(createLog(u5, "Lyrid meteors",
                "Caught 12 in 45 minutes. One left a persistent train.",
                base.plus(11, ChronoUnit.DAYS), "Shenandoah", 38.5, -78.4, 4, "Clear", 7, true));
        logs.add(createLog(u1, "Whirlpool Galaxy M51",
                "Spiral structure with 10-inch. Companion NGC 5195 clear.",
                base.plus(9, ChronoUnit.DAYS), "Kitt Peak area", 31.9, -111.6, 2, "Clear", 8, true));
        logs.add(createLog(u2, "Eagle Nebula pillars",
                "Pillars of Creation region. 12-inch Dob, O-III filter.",
                base.plus(7, ChronoUnit.DAYS), "Davis Mountains, TX", 30.6, -104.0, 2, "Clear", 9, true));
        logs.add(createLog(u3, "Double star night",
                "Albireo, Mizar-Alcor, Epsilon Lyrae. Good for showing beginners.",
                base.plus(6, ChronoUnit.DAYS), "Local club dark site", 39.0, -76.5, 4, "Clear", 7, true));
        logs.add(createLog(u4, "Moon and Jupiter conjunction",
                "Close approach, both in same 2° field. Nice photo op.",
                base.plus(5, ChronoUnit.DAYS), "Backyard", 32.8, -97.0, 6, "Clear", 6, true));
        logs.add(createLog(u5, "Veil Nebula complex",
                "Eastern and Western Veil in O-III. Breathtaking. 4-inch with 2-inch O-III.",
                base.plus(4, ChronoUnit.DAYS), "Glacier NP", 48.8, -113.8, 1, "Clear", 9, true));
        logs.add(createLog(u1, "Solar prominence sketch",
                "Two nice prominences on the limb. White light + H-alpha.",
                base.plus(3, ChronoUnit.DAYS), "Driveway", 40.0, -83.0, 6, "Clear", 6, true));

        return observationLogRepository.saveAll(logs);
    }

    private ObservationLog createLog(User user, String title, String description,
                                    Instant observedAt, String locationName,
                                    double lat, double lng, int bortle, String weather, int seeing, boolean isPublic) {
        ObservationLog log = new ObservationLog();
        log.setUser(user);
        log.setTitle(title);
        log.setDescription(description);
        log.setObservedAt(observedAt);
        log.setLocationName(locationName);
        log.setLat(lat);
        log.setLng(lng);
        log.setBortleScale(bortle);
        log.setWeatherCondition(weather);
        log.setSeeingRating(seeing);
        log.setIsPublic(isPublic);
        return log;
    }

    private void seedLikesAndComments(List<User> users, List<ObservationLog> logs) {
        User u1 = users.get(0);
        User u2 = users.get(1);
        User u3 = users.get(2);
        User u4 = users.get(3);
        User u5 = users.get(4);
        ObservationLog log1 = logs.get(0);
        ObservationLog log2 = logs.get(1);
        ObservationLog log3 = logs.get(2);
        ObservationLog log5 = logs.get(4);
        ObservationLog log9 = logs.get(8);
        ObservationLog log10 = logs.get(9);
        ObservationLog log12 = logs.get(11);

        logLikeRepository.save(new LogLike(log1, u2));
        logLikeRepository.save(new LogLike(log1, u3));
        logLikeRepository.save(new LogLike(log2, u1));
        logLikeRepository.save(new LogLike(log2, u3));
        logLikeRepository.save(new LogLike(log3, u1));
        logLikeRepository.save(new LogLike(log3, u2));
        logLikeRepository.save(new LogLike(log5, u1));
        logLikeRepository.save(new LogLike(log9, u1));
        logLikeRepository.save(new LogLike(log9, u3));
        logLikeRepository.save(new LogLike(log10, u2));
        logLikeRepository.save(new LogLike(log10, u4));
        logLikeRepository.save(new LogLike(log12, u5));

        commentRepository.save(new Comment(log1, u2, "Nice report. What eyepiece did you use for the bands?"));
        commentRepository.save(new Comment(log1, u1, "25mm Plossl for the full disk, 10mm for the moons."));
        commentRepository.save(new Comment(log2, u3, "Cherry Springs is on my bucket list. How was the drive?"));
        commentRepository.save(new Comment(log3, u2, "87 in 90 min is a great count. We had clouds after midnight here."));
        commentRepository.save(new Comment(log9, u2, "M13 is one of my favorites. Great report!"));
        commentRepository.save(new Comment(log10, u1, "Death Valley is on my list for next year. How was the humidity?"));
    }
}
