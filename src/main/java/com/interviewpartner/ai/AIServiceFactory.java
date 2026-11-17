package com.interviewpartner.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpartner.ai.impl.OpenRouterWaveAIService;
import com.interviewpartner.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AIServiceFactory {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;

    @Bean
    public WaveAIService waveAIService() {
        String provider = aiConfig.getProvider();
        log.info("Initializing AI provider: {}", provider);

        if (!"openrouter".equalsIgnoreCase(provider)) {
            log.error("Unsupported AI provider: {}. Only 'openrouter' is supported.", provider);
            throw new IllegalStateException("Unsupported AI provider: " + provider + ". Only 'openrouter' is supported.");
        }

        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isEmpty()) {
            log.error("OpenRouter API key is required but not provided!");
            throw new IllegalStateException("OpenRouter API key is required. Please set app.ai.api-key in application.yml");
        }

        log.info("Initializing OpenRouter with model: {}", aiConfig.getModel());
        return new OpenRouterWaveAIService(aiConfig, objectMapper);
    }
}