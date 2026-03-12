package com.astroscout.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;

@SpringBootApplication(exclude = {
		OpenAiAudioSpeechAutoConfiguration.class,
		OpenAiAudioTranscriptionAutoConfiguration.class,
		OpenAiChatAutoConfiguration.class,   // Loaded by ConditionalOpenAiChatConfig when provider != anthropic
		OpenAiEmbeddingAutoConfiguration.class,
		OpenAiImageAutoConfiguration.class,
		OpenAiModerationAutoConfiguration.class,
		AnthropicChatAutoConfiguration.class  // Loaded by ConditionalAnthropicChatConfig when provider=anthropic
})
public class AstroscoutBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AstroscoutBackendApplication.class, args);
	}

}
