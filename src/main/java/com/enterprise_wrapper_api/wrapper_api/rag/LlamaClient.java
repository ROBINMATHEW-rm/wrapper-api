package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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
     * Blocking call - returns full answer at once
     */
    public String generateAnswer(String prompt) {
        return generateAnswer(prompt, 0.2);
    }

    public String generateAnswer(String prompt, double temperature) {
        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", temperature,
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

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /**
     * Streaming call - returns tokens one by one as Flux<String>
     */
    public Flux<String> generateAnswerStream(String prompt, double temperature) {
        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", temperature,
                "max_tokens", 500,
                "stream", true  // Enable streaming
        );

        return webClient.post()
                .uri("/openai/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)  // Each chunk is a raw SSE line
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> {
                    // Parse the token from: data: {"choices":[{"delta":{"content":"token"}}]}
                    try {
                        String json = line.substring(6); // remove "data: "
                        // Extract content from delta using simple string parsing
                        int contentIdx = json.indexOf("\"content\":\"");
                        if (contentIdx == -1) return "";
                        int start = contentIdx + 11;
                        int end = json.indexOf("\"", start);
                        if (end == -1) return "";
                        String token = json.substring(start, end);
                        // Unescape common JSON escape sequences
                        token = token.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");
                        return token;
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(token -> !token.isEmpty());
    }
}
