package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrieverService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public RetrieverService(EmbeddingService embeddingService,
                            VectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public List<String> retrieveRelevantDocs(String query, int topK) {
        return retrieveRelevantDocs(query, topK, null, null);
    }

    public List<String> retrieveRelevantDocs(String query, int topK, String documentId) {
        return retrieveRelevantDocs(query, topK, documentId, null);
    }

    public List<String> retrieveRelevantDocs(String query, int topK, String documentId, Double threshold) {
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        
        if (threshold != null) {
            return vectorStoreService.search(queryEmbedding, topK, documentId, threshold);
        } else {
            return vectorStoreService.search(queryEmbedding, topK, documentId);
        }
    }
}
