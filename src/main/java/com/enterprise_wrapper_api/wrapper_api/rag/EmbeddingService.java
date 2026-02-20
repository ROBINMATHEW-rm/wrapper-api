package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final LocalEmbeddingService localEmbeddingService;

    @Autowired
    public EmbeddingService(LocalEmbeddingService localEmbeddingService) {
        this.localEmbeddingService = localEmbeddingService;
    }

    public List<Double> generateEmbedding(String text) {
        try {
            // Truncate text if too long
            String truncatedText = text.length() > 8000 ? text.substring(0, 8000) : text;
            
            // Use local embedding generation
            return localEmbeddingService.generateEmbedding(truncatedText);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
}
