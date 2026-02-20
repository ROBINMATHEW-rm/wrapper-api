package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private final WebClient webClient;
    private static final String EMBEDDING_MODEL = "nomic-embed-text";

    public EmbeddingService(
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

    public List<Double> generateEmbedding(String text) {
        try {
            // Truncate text if too long (most embedding models have token limits)
            String truncatedText = text.length() > 8000 ? text.substring(0, 8000) : text;

            Map<String, Object> body = Map.of(
                    "model", EMBEDDING_MODEL,
                    "input", truncatedText
            );

            Map<String, Object> response = webClient.post()
                    .uri("/openai/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Number> embeddingNumbers = (List<Number>) data.get(0).get("embedding");

            return embeddingNumbers.stream()
                    .map(Number::doubleValue)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
}
