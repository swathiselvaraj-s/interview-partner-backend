package com.interviewpartner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AIConfig {
    private String provider = "openrouter";
    private String apiKey;
    private String model = "openai/gpt-3.5-turbo";
    private double temperature = 0.7;
    private int maxTokens = 2000;
    private int timeout = 30000;
}