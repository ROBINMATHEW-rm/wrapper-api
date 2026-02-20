package com.enterprise_wrapper_api.wrapper_api.service.impl;

import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchRequest;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    private final Tika tika = new Tika();

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

    // =============================
    // PUBLIC METHODS
    // =============================

    public ResumeMatchResponse getMatch(ResumeMatchRequest request) {
        try {
            String prompt = buildPrompt(
                    request.getResumeText(),
                    request.getJobDescription()
            );

            Map<String, Object> body = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2,
                    "max_tokens", 500,
                    "response_format", Map.of("type", "json_object")
            );

            Map<String, Object> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseGroqResponse(response);

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

    public ResumeMatchResponse matchUploadedResume(
            MultipartFile file,
            String jobDescription) {

        try {
            validateFile(file);

            String resumeText =
                    tika.parseToString(file.getInputStream());

            ResumeMatchRequest request = new ResumeMatchRequest();
            request.setResumeText(resumeText);
            request.setJobDescription(jobDescription);

            return getMatch(request);

        } catch (Exception e) {
            return new ResumeMatchResponse(
                    0,
                    Collections.emptyList(),
                    "Failed to process resume file"
            );
        }
    }

    // =============================
    // PRIVATE HELPERS
    // =============================

    private String buildPrompt(String resumeText, String jobDescription) {
        return """
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
                """.formatted(resumeText, jobDescription);
    }

    private ResumeMatchResponse parseGroqResponse(
            Map<String, Object> response) throws Exception {

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        String content = (String) message.get("content");

        Map<String, Object> json =
                objectMapper.readValue(content, Map.class);

        int matchScore =
                ((Number) json.getOrDefault("matchScore", 0)).intValue();

        List<String> missingSkills =
                (List<String>) json.getOrDefault(
                        "missingSkills", Collections.emptyList());

        String summary =
                (String) json.getOrDefault("summary", "");

        return new ResumeMatchResponse(
                matchScore,
                missingSkills,
                summary
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB");
        }

        String type = file.getContentType();
        if (type == null ||
                !(type.equals("application/pdf")
                        || type.equals("application/msword")
                        || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {

            throw new IllegalArgumentException(
                    "Only PDF or DOC/DOCX files are allowed");
        }
    }
}
