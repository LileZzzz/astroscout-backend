package com.astroscout.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;

/**
 * Load Anthropic Chat when astroscout.ai.provider=anthropic and api-key is set.
 * Use with MiniMax Anthropic-compatible endpoint (e.g. base-url=https://api.minimaxi.com/anthropic).
 */
@Configuration
@ConditionalOnProperty(name = "astroscout.ai.provider", havingValue = "anthropic")
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
@Import(AnthropicChatAutoConfiguration.class)
public class ConditionalAnthropicChatConfig {
}
