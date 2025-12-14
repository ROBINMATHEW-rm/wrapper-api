package com.enterprise_wrapper_api.wrapper_api.service.impl;

import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchRequest;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ResumeMatchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResumeMatchService(
            WebClient.Builder builder,
            @Value("${groq.api.url}") String groqUrl,
            @Value("${groq.api.key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.webClient = builder
                .baseUrl(groqUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = objectMapper;
    }

    public ResumeMatchResponse getMatch(ResumeMatchRequest request) {
        String prompt = """
                Compare the resume with the job description.
                Respond ONLY in valid JSON:
                {
                  "matchScore": number between 0 and 100,
                  "missingSkills": ["skill1", "skill2"],
                  "summary": "2 sentences"
                }

                Resume:
                %s

                Job Description:
                %s
                """.formatted(request.getResumeText(), request.getJobDescription());

        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2,
                "max_tokens", 500,
                "response_format", Map.of("type", "json_object")
        );

        try {
            Map<String, Object> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // ✅ Parse JSON string returned by the model into ResumeMatchResponse
            Map<String, Object> jsonResponse = objectMapper.readValue(content, Map.class);

            int matchScore = (int) (jsonResponse.getOrDefault("matchScore", 0));
            List<String> missingSkills = (List<String>) jsonResponse.getOrDefault("missingSkills", Collections.emptyList());
            String summary = (String) jsonResponse.getOrDefault("summary", "");

            return new ResumeMatchResponse(matchScore, missingSkills, summary);

        } catch (WebClientResponseException e) {
            return new ResumeMatchResponse(
                    0,
                    Collections.emptyList(),
                    "GROQ ERROR: " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            return new ResumeMatchResponse(
                    0,
                    Collections.emptyList(),
                    "ERROR: " + e.getMessage()
            );
        }
    }
}
