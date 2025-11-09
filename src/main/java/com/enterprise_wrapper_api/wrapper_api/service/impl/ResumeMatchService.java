package com.enterprise_wrapper_api.wrapper_api.service.impl;

import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchRequest;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ResumeMatchService {

    private final WebClient webClient;

    public ResumeMatchService(
            WebClient.Builder builder,
            @Value("${openai.api.url}") String openAiUrl,
            @Value("${openai.api.key}") String apiKey) {

        this.webClient = builder
                .baseUrl(openAiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public ResumeMatchResponse getMatch(ResumeMatchRequest request) {

        // ✅ Build the AI prompt
        String prompt = """
                Compare the following resume text with the job description.
                Return a JSON with:
                {
                  "matchScore": number between 0 and 100,
                  "missingSkills": ["skill1", "skill2", ...],
                  "summary": "2 sentences summary of fit"
                }

                Resume:
                %s

                Job Description:
                %s
                """.formatted(request.getResumeText(), request.getJobDescription());

        // ✅ Request body for OpenAI API
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",  // faster and cheaper model
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        // ✅ Send request and get response
        Map<String, Object> response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // ✅ Extract content safely
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        String content = (String) message.get("content");

        // ✅ Return AI response
        return new ResumeMatchResponse(0, List.of(), content);
    }
}
