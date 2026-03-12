package com.astroscout.backend.ai;

import com.astroscout.backend.observation.CelestialCatalogService;

import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RAG-style astronomy Q&amp;A: injects celestial catalog context into the prompt
 * and calls the LLM. Without PGVector we use the full catalog as context;
 * later can switch to embedding + vector search for Top-K.
 * <p>
 * When using Anthropic (e.g. MiniMax), the provider stores the raw response in
 * {@code response.getMetadata().get("anthropic-response")}. We use {@link #extractTextOnlyFromAnthropicResponse}
 * to read only TEXT content blocks and ignore THINKING, so the user sees the reply without reasoning.
 * If that is not available (e.g. OpenAI provider), we fall back to {@link #stripReasoningPrefix}.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final String SYSTEM_CONTEXT = """
            You are a friendly astronomy assistant for AstroScout. Answer questions about observing,
            celestial objects, and stargazing based on the following reference data. Keep answers concise
            and practical. If the user asks about visibility or location, use the catalog below; if they
            ask about something not in the data, say so and give general advice where appropriate.

            Important: Reply ONLY with the direct answer or greeting to the user. Do not include your
            reasoning, planning, or meta-commentary (e.g. do not say "The user has sent...", "I should respond...").
            Use plain text only: no Markdown (no **, ##, ```, or other formatting). Output just what the user should see.
            """;

    private final ChatModel chatModel;
    private final CelestialCatalogService catalogService;

    public AiChatService(CelestialCatalogService catalogService,
                         @Autowired(required = false) ChatModel chatModel) {
        this.catalogService = catalogService;
        this.chatModel = chatModel;
    }

    /**
     * Process user message with RAG-style context (catalog as in-prompt context).
     * If no LLM is configured (missing API key), returns a stub message.
     */
    public String chat(String userMessage) {
        if (chatModel == null) {
            return "AI chat is not configured. Set spring.ai.openai.api-key (or OPENAI_API_KEY) to enable astronomy Q&A.";
        }
        try {
            String context = catalogService.getCatalogContextForRag();
            String systemContent = SYSTEM_CONTEXT + "\n\nReference celestial data:\n" + context;

            Prompt prompt = new Prompt(
                    new SystemMessage(systemContent),
                    new UserMessage(userMessage)
            );
            ChatResponse response = chatModel.call(prompt);
            // Spring AI Anthropic puts raw ChatCompletionResponse in metadata; extract only TEXT blocks (no thinking).
            String textOnly = extractTextOnlyFromAnthropicResponse(response);
            if (textOnly != null && !textOnly.isBlank()) {
                return toPlainText(textOnly);
            }
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
            String fromMeta = tryGetTextOnlyFromMetadata(result);
            if (fromMeta != null && !fromMeta.isBlank()) {
                return toPlainText(fromMeta);
            }
            return toPlainText(stripReasoningPrefix(Objects.requireNonNull(text)));
        } catch (Exception e) {
            log.warn("AI chat failed for user message: {}", userMessage, e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("429") || msg.toLowerCase().contains("rate limit")) {
                return "The free model is rate limited. Please wait a moment and try again.";
            }
            return "The astronomy assistant is temporarily unavailable. Please try again later. (Error: " + msg + ")";
        }
    }

    /**
     * When using Anthropic provider, Spring AI stores the raw ChatCompletionResponse in
     * response metadata under "anthropic-response". We can read only TEXT/TEXT_DELTA
     * content blocks and ignore THINKING, so the user sees the reply without reasoning.
     * See AnthropicChatModel.toChatResponse() which does keyValue("anthropic-response", chatCompletion).
     */
    private String extractTextOnlyFromAnthropicResponse(ChatResponse response) {
        if (response == null || response.getMetadata() == null) return null;
        Object raw = response.getMetadata().get("anthropic-response");
        if (!(raw instanceof AnthropicApi.ChatCompletionResponse ar)) return null;
        if (ar.content() == null) return null;
        String text = ar.content().stream()
                .filter(c -> c.type() == AnthropicApi.ContentBlock.Type.TEXT
                        || c.type() == AnthropicApi.ContentBlock.Type.TEXT_DELTA)
                .map(AnthropicApi.ContentBlock::text)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));
        return text.isBlank() ? null : text;
    }

    /**
     * If the provider put "text only" (no reasoning) in generation metadata, use it.
     */
    private String tryGetTextOnlyFromMetadata(Generation result) {
        if (result == null || result.getMetadata() == null) return null;
        // Some providers may expose text-only content via metadata (e.g. native response mapping)
        Object v = result.getMetadata().get("text");
        if (v instanceof String s && !s.isBlank()) return s;
        v = result.getMetadata().get("textContent");
        if (v instanceof String s && !s.isBlank()) return s;
        return null;
    }

    /**
     * Strip MiniMax/Anthropic-style reasoning prefix (e.g. "The user has simply said... I should respond...").
     * Keeps only the direct reply to show the user. Used when the API returns merged thinking+text in getText().
     */
    private String stripReasoningPrefix(String text) {
        if (text == null || text.isBlank()) return "";
        String t = text.strip();
        // If no obvious reasoning, return as-is
        if (!t.contains("The user has") && !t.contains("I should respond") && !t.contains("I'll keep it")
                && !t.contains("they're just ") && !t.contains("I'll ")) {
            return text;
        }
        // Split by double newline; take the last segment that looks like a direct reply
        String[] segments = t.split("\n\n+");
        for (int i = segments.length - 1; i >= 0; i--) {
            String seg = segments[i].strip();
            if (seg.isEmpty()) continue;
            boolean looksLikeReasoning = seg.startsWith("The user has") || seg.startsWith("I should ")
                    || seg.startsWith("I'll keep") || seg.startsWith("I'll ") || seg.startsWith("they're ");
            if (!looksLikeReasoning) {
                return seg;
            }
        }
        // Try to find first line that looks like a greeting/reply (Hi, Hello, Hey, ...)
        for (String line : t.split("\n")) {
            String l = line.strip();
            if (l.isEmpty()) continue;
            if (l.startsWith("Hi") || l.startsWith("Hello") || l.startsWith("Hey") || l.startsWith("Greetings")
                    || l.startsWith("Hi there") || l.matches("^[A-Z][a-z]+!?\\s.*")) {
                int idx = t.indexOf(l);
                return idx >= 0 ? t.substring(idx).strip() : text;
            }
        }
        // Entire response was reasoning with no actual reply – return a short fallback greeting
        if (t.startsWith("The user has") || t.startsWith("I should ") || t.contains("I'll keep it")) {
            return "Hello! I'm the AstroScout astronomy assistant. How can I help you with the night sky today?";
        }
        return text;
    }

    /** Strip Markdown to plain text so the chat UI shows clean text. */
    private String toPlainText(String text) {
        if (text == null || text.isBlank()) return text;
        String s = text
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                .replaceAll("\\*([^*]+)\\*", "$1")
                .replaceAll("__([^_]+)__", "$1")
                .replaceAll("_([^_]+)_", "$1")
                .replaceAll("^#+\\s*", "")
                .replaceAll("\\n#+\\s*", "\n")
                .replaceAll("(?s)```\\w*\\n?(.*?)```", "$1")
                .replaceAll("\\[([^]]+)\\]\\([^)]+\\)", "$1");
        return s.strip();
    }
}
