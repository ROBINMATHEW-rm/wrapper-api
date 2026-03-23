package com.enterprise_wrapper_api.wrapper_api.rag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LlamaClient {

    private final WebClient webClient;

    public LlamaClient(
            WebClient.Builder builder,
            @Value("${groq.api.base-url}") String groqUrl,
            @Value("${groq.api.key}") String apiKey
    ) {

        this.webClient = builder
                .baseUrl(groqUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Sends prompt to Groq LLaMA and returns generated text
     */
    public String generateAnswer(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 500
        );

        Map<String, Object> response = webClient.post()
                .uri("/openai/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }
}
