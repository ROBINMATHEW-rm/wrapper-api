package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;

    public EmbeddingService(
            WebClient.Builder builder,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaUrl
    ) {
        this.webClient = builder
                .baseUrl(ollamaUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public List<Double> generateEmbedding(String text) {
        String truncated = text.length() > 8000 ? text.substring(0, 8000) : text;

        Map<String, Object> body = Map.of(
                "model", "nomic-embed-text",
                "prompt", truncated
        );

        Map<String, Object> response = webClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .block();

        return (List<Double>) response.get("embedding");
    }
}
