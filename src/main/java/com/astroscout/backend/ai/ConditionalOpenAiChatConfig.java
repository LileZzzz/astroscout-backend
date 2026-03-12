package com.astroscout.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;

/**
 * Load OpenAI Chat when api-key is set and provider is not "anthropic".
 * Use astroscout.ai.provider=openai (or omit) for MiniMax OpenAI-style; use anthropic for Anthropic-style.
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.api-key")
@ConditionalOnProperty(name = "astroscout.ai.provider", havingValue = "openai", matchIfMissing = true)
@Import(OpenAiChatAutoConfiguration.class)
public class ConditionalOpenAiChatConfig {
}
