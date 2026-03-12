package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Bridge for Spring AI 1.0 (built against Spring Boot 3) on Spring Boot 4.
 * Spring AI's OpenAI auto-config references this class name; in SB4 the actual
 * config moved to org.springframework.boot.webclient.autoconfigure.
 */
@Configuration
@Import(org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration.class)
public class WebClientAutoConfiguration {
}
