package com.astroscout.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;

/**
 * Load OpenAI Chat when api-key is set.
 */
@Configuration
@ConditionalOnExpression("'${spring.ai.openai.api-key:}' != ''")
@Import(OpenAiChatAutoConfiguration.class)
public class ConditionalOpenAiChatConfig {
}
