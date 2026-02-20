package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class EmbeddingService {

    public List<Double> generateEmbedding(String text) {

        // Deterministic seed so same text → same embedding
        Random random = new Random(text.hashCode());

        List<Double> embedding = new ArrayList<>();

        for (int i = 0; i < 768; i++) {
            embedding.add((double) random.nextFloat());
        }

        return embedding;
    }
}
