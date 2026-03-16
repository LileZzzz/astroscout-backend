package com.astroscout.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;

@SpringBootApplication(exclude = {
		OpenAiAudioSpeechAutoConfiguration.class,
		OpenAiAudioTranscriptionAutoConfiguration.class,
		OpenAiChatAutoConfiguration.class,   // Loaded by ConditionalOpenAiChatConfig when api key is present
		OpenAiEmbeddingAutoConfiguration.class,
		OpenAiImageAutoConfiguration.class,
		OpenAiModerationAutoConfiguration.class
})
public class AstroscoutBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AstroscoutBackendApplication.class, args);
	}

}
