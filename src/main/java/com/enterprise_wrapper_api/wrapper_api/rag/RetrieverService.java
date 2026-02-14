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

        // Generate embedding for query
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        // Search in-memory vector store
        return vectorStoreService.search(queryEmbedding, topK);
    }
}
