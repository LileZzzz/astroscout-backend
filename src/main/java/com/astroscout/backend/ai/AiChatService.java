package com.astroscout.backend.ai;

import com.astroscout.backend.observation.BestWindowService;
import com.astroscout.backend.observation.CelestialCatalogService;
import com.astroscout.backend.observation.ObservationScoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Astronomy chat based on model knowledge with optional planner context hydration.
 * <p>
 * Plain text output is controlled by the system prompt. We intentionally avoid
 * response post-processing transforms.
 */
@Service
public class AiChatService {

    private static final List<Pattern> PLANNING_INTENT_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(weather|cloud|humidity|visibility|wind|moon phase|light pollution|bortle)\\b"),
            Pattern.compile("\\b(observe|observation|observing|stargazing|sky conditions|seeing conditions)\\b"),
            Pattern.compile("\\b(plan|planner|recommend|recommendation|backyard stargazing|session|time window|best window)\\b"),
            Pattern.compile("\\b(what can i see tonight|can i see|is tonight good|should i observe|what targets)\\b"),
            Pattern.compile("天气|云量|湿度|能见度|风速|月相|光污染|观测|观星|今晚|目标|时间窗口|适合观测")
    );

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final String SYSTEM_CONTEXT = """
            You are AstroScoutAssistant, an in-product astronomy assistant for AstroScout.
            Answer astronomy questions, observation-planning questions, and night-sky viewing questions.
            Use your general astronomy knowledge by default.
            When planner context is provided, treat it as the source of truth for local observing conditions and combine it with your astronomy knowledge.

            Do not fabricate location-specific weather, visibility, or observing-window details when context is missing.
            If planner context is available, use it to explain whether observing is a good idea, what targets make sense, and what time window is best.
            If no planner context is available and the question is general, answer normally from your astronomy knowledge.

            Style requirements:
            Start with the answer itself. Do not add greetings, pleasantries, or filler unless the user explicitly greets you first.
            Keep the tone concise, factual, and product-like.
            Prefer short paragraphs over lists unless the user asks for steps or options.

            Important: Reply ONLY with the direct answer to the user. Do not include your
            reasoning, planning, or meta-commentary (e.g. do not say "The user has sent...", "I should respond...").
            Use plain text only: no Markdown (no **, ##, ```, or other formatting). Output just what the user should see.
            """;

    private static final String MISSING_PLANNER_CONTEXT_TEMPLATE =
            "To give an accurate observing answer, share your latitude, longitude, and date (YYYY-MM-DD). "
                    + "Then I can check weather, sky conditions, best observing windows, and suggested targets for you.";

    private final ChatModel chatModel;
    private final CelestialCatalogService catalogService;
    private final ObservationScoreService observationScoreService;
    private final BestWindowService bestWindowService;

    public AiChatService(CelestialCatalogService catalogService,
                         ObservationScoreService observationScoreService,
                         BestWindowService bestWindowService,
                         @Autowired(required = false) ChatModel chatModel) {
        this.catalogService = catalogService;
        this.observationScoreService = observationScoreService;
        this.bestWindowService = bestWindowService;
        this.chatModel = chatModel;
    }

    /**
     * Process user message with optional planner context.
     */
    public String chat(String userMessage) {
        ChatRequest request = new ChatRequest(userMessage, null, null, null, null, null, null, null, null, null);
        return chat(request);
    }

    /**
     * Process user message with optional planner context (lat/lng/date/weather/score/targets)
     * injected into the prompt, so responses can be practical and location-aware.
     */
    public String chat(ChatRequest request) {
        if (chatModel == null) {
            return "AI chat is not configured. Configure Gemini AI Studio OpenAI-compatible settings and API key before starting backend.";
        }

        String userText = request != null && request.message() != null ? request.message() : "";
        String safeUserText = userText.isBlank() ? "Please help with astronomy." : userText;

        HydratedPlannerContext hydrated = hydratePlannerContext(request);
        if (hydrated.requirePlannerPrompt()) {
            return MISSING_PLANNER_CONTEXT_TEMPLATE;
        }

        try {
            String plannerContext = buildPlannerContext(hydrated);
            String systemContent = SYSTEM_CONTEXT
                    + "\n\nPlanner context (if provided by user workflow):\n"
                    + plannerContext
                    + "\n\nWhen planner context is present, prioritize practical suggestions based on that context.";

            Prompt prompt = new Prompt(
                    new SystemMessage(systemContent),
                    new UserMessage(safeUserText)
            );
            ChatResponse response = chatModel.call(prompt);
            Generation result = response.getResult();
            if (result == null) {
                return "I couldn't generate a response. Please try rephrasing your question.";
            }
            AssistantMessage output = result.getOutput();
            if (output == null) {
                return "I couldn't generate a response. Please try rephrasing your question.";
            }
            String text = output.getText();
            if (text == null || text.isBlank()) {
                return "I couldn't generate a response. Please try rephrasing your question.";
            }
            return text;
        } catch (Exception e) {
            log.warn("AI chat failed for user message: {}", userText, e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            String lower = msg.toLowerCase();
            if (msg.contains("429") || msg.toLowerCase().contains("rate limit")) {
                return "The free model is rate limited. Please wait a moment and try again.";
            }
            if (msg.contains("401") || lower.contains("unauthorized") || lower.contains("invalid api key")) {
                return "AI provider authentication failed (401). Verify your Gemini AI Studio API key and ensure the key is loaded in environment variables.";
            }
            if (msg.contains("404") || lower.contains("model not found")) {
                return "AI provider endpoint/model was not found (404). Verify Gemini OpenAI-compatible base URL and model name.";
            }
            return "The astronomy assistant is temporarily unavailable. Please try again later. (Error: " + msg + ")";
        }
    }

    private HydratedPlannerContext hydratePlannerContext(ChatRequest request) {
        if (request == null) {
            return new HydratedPlannerContext(true, null, null, null, null, null, null, null, null);
        }

        String message = request.message() == null ? "" : request.message();
        boolean needsPlannerContext = requiresPlannerContext(message);

        Double lat = request.lat();
        Double lng = request.lng();
        LocalDate date = parseDate(request.date());

        boolean hasValidCoordinates = lat != null && lng != null && isValidLat(lat, lng);
        boolean hasLocationDate = hasValidCoordinates && date != null;

        if (needsPlannerContext && !hasLocationDate) {
            return new HydratedPlannerContext(true, null, null, null, null, null, null, null, null);
        }

        Double score = request.score();
        String weatherSummary = request.weatherSummary();
        String moonPhaseLabel = request.moonPhaseLabel();
        Integer bortleScale = request.bortleScale();
        String targetSummary = request.targetSummary();
        String bestWindowSummary = request.bestWindowSummary();

        if (!hasLocationDate) {
            return new HydratedPlannerContext(false, null, null, null, null, null, null, null, null);
        }

        if (lat == null || lng == null || date == null) {
            return new HydratedPlannerContext(false, null, null, null, null, null, null, null, null);
        }

        double resolvedLat = lat.doubleValue();
        double resolvedLng = lng.doubleValue();
        LocalDate resolvedDate = date;

        boolean missingScore = score == null;
        boolean missingWeather = isBlank(weatherSummary);
        boolean missingMoon = isBlank(moonPhaseLabel);
        boolean missingBortle = bortleScale == null;
        boolean missingTargets = isBlank(targetSummary);
        boolean missingWindows = isBlank(bestWindowSummary);

        if (missingScore || missingWeather || missingMoon || missingBortle || missingTargets || missingWindows) {
            try {
                ObservationScoreService.ScoreBreakdown breakdown = observationScoreService.score(resolvedLat, resolvedLng, resolvedDate);

                if (missingScore) {
                    score = Math.round(breakdown.totalScore() * 10.0) / 10.0;
                }
                if (missingWeather) {
                    weatherSummary = formatWeatherSummary(breakdown.weather());
                }
                if (missingMoon) {
                    moonPhaseLabel = breakdown.astronomy().moonPhaseLabel();
                }
                if (missingBortle) {
                    bortleScale = breakdown.lightPollution().bortleScale();
                }
                if (missingTargets) {
                    List<CelestialCatalogService.VisibleObject> visible = catalogService.computeVisible(resolvedLat, resolvedLng, resolvedDate, null);
                    targetSummary = formatTargetSummary(visible);
                }
                if (missingWindows) {
                    List<BestWindowService.BestWindow> windows = bestWindowService.computeBestWindows(resolvedLat, resolvedLng, resolvedDate);
                    bestWindowSummary = formatWindowSummary(windows);
                }
            } catch (Exception e) {
                log.warn("Planner context hydration failed for lat={}, lng={}, date={}", lat, lng, date, e);
            }
        }

        return new HydratedPlannerContext(
                false,
                lat,
                lng,
                date,
                score,
                weatherSummary,
                moonPhaseLabel,
                bortleScale,
                mergeTargetAndWindowSummary(targetSummary, bestWindowSummary)
        );
    }

    private String buildPlannerContext(HydratedPlannerContext context) {
        if (context == null || context.lat() == null || context.lng() == null || context.date() == null) {
            return "No planner context provided.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- Coordinates: lat=").append(context.lat())
                .append(", lng=").append(context.lng()).append("\n");
        sb.append("- Date: ").append(context.date()).append("\n");

        if (context.score() != null) {
            sb.append("- Observe score: ").append(context.score()).append("/100\n");
        }
        if (!isBlank(context.weatherSummary())) {
            sb.append("- Weather summary: ").append(context.weatherSummary()).append("\n");
        }
        if (!isBlank(context.moonPhaseLabel())) {
            sb.append("- Moon phase: ").append(context.moonPhaseLabel()).append("\n");
        }
        if (context.bortleScale() != null) {
            sb.append("- Bortle scale: ").append(context.bortleScale()).append("\n");
        }
        if (!isBlank(context.targetAndWindowSummary())) {
            sb.append(context.targetAndWindowSummary()).append("\n");
        }

        if (sb.isEmpty()) {
            return "No planner context provided.";
        }
        return sb.toString().strip();
    }

    private boolean requiresPlannerContext(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return false;
        }
        return PLANNING_INTENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
    }

    private boolean isValidLat(double lat, double lng) {
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    private LocalDate parseDate(String dateText) {
        if (isBlank(dateText)) {
            return null;
        }
        try {
            return LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatWeatherSummary(ObservationScoreService.WeatherData weather) {
        return "cloud=" + weather.cloudCoverPercent() + "%, visibility=" + weather.visibilityKm() + "km, humidity="
                + weather.humidityPercent() + "%, wind=" + weather.windSpeedMps() + "m/s";
    }

    private String formatTargetSummary(List<CelestialCatalogService.VisibleObject> visible) {
        if (visible == null || visible.isEmpty()) {
            return "No suggested targets available.";
        }
        return visible.stream()
                .limit(5)
                .map(v -> v.name() + " (" + v.type() + ", alt " + Math.round(v.altDeg()) + " deg, mag "
                        + String.format(Locale.ROOT, "%.1f", v.magnitude()) + ")")
                .reduce((a, b) -> a + "; " + b)
                .orElse("No suggested targets available.");
    }

    private String formatWindowSummary(List<BestWindowService.BestWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            return "No best observing windows available.";
        }
        return windows.stream()
                .limit(3)
                .map(w -> w.start() + "-" + w.end() + " (" + w.quality() + ", " + w.reason() + ")")
                .reduce((a, b) -> a + "; " + b)
                .orElse("No best observing windows available.");
    }

    private String mergeTargetAndWindowSummary(String targetSummary, String windowSummary) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(windowSummary)) {
            sb.append("- Best observing windows: ").append(windowSummary).append("\n");
        }
        if (!isBlank(targetSummary)) {
            sb.append("- Suggested targets: ").append(targetSummary);
        }
        return sb.toString().strip();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record HydratedPlannerContext(
            boolean requirePlannerPrompt,
            Double lat,
            Double lng,
            LocalDate date,
            Double score,
            String weatherSummary,
            String moonPhaseLabel,
            Integer bortleScale,
            String targetAndWindowSummary
    ) {}
}
