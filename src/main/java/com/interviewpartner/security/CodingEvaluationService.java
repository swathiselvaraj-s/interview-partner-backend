package com.interviewpartner.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

//@Service
@RequiredArgsConstructor
@Slf4j
public class CodingEvaluationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${judge0.api.url:https://judge0-ce.p.rapidapi.com}")
    private String judge0Url;

    @Value("${judge0.api.key:}")
    private String apiKey;

    public Map<String, Object> evaluateCode(String code, String language, String stdin) {
        try {
            Map<String, Integer> languageIds = Map.of(
                    "java", 62,
                    "python", 71,
                    "javascript", 63,
                    "cpp", 54
            );

            Integer languageId = languageIds.get(language.toLowerCase());
            if (languageId == null) {
                throw new IllegalArgumentException("Unsupported language: " + language);
            }

            // Build request with proper type safety
            Map<String, Object> request = new HashMap<>();
            request.put("source_code", code);
            request.put("language_id", languageId);
            request.put("stdin", stdin != null ? stdin : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("X-RapidAPI-Key", apiKey);
                headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Submit code - using ParameterizedTypeReference for type safety
            ResponseEntity<Map<String, Object>> submitResponse = restTemplate.exchange(
                    judge0Url + "/submissions",
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> submitBody = submitResponse.getBody();
            if (submitBody == null || !submitBody.containsKey("token")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to submit code");
                return errorResponse;
            }

            String token = (String) submitBody.get("token");

            // Wait for result
            Thread.sleep(2000);

            // Get result
            ResponseEntity<Map<String, Object>> resultResponse = restTemplate.exchange(
                    judge0Url + "/submissions/" + token + "?base64_encoded=false",
                    HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = resultResponse.getBody();
            if (result == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No result from judge");
                return errorResponse;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("output", result.get("stdout"));
            response.put("error", result.get("stderr"));
            response.put("status", result.get("status"));
            response.put("time", result.get("time"));
            response.put("memory", result.get("memory"));

            return response;

        } catch (Exception e) {
            log.error("Error evaluating code: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }
}