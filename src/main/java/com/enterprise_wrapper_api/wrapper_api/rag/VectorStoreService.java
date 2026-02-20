package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VectorStoreService {

    private static class VectorEntry {
        private final String content;
        private final List<Double> embedding;

        public VectorEntry(String content, List<Double> embedding) {
            this.content = content;
            this.embedding = embedding;
        }

        public String getContent() {
            return content;
        }

        public List<Double> getEmbedding() {
            return embedding;
        }
    }

    private final List<VectorEntry> store = new ArrayList<>();
    public void clear() {
        store.clear();
    }
    // Store chunk + embedding
    public void store(String content, List<Double> embedding) {
        System.out.println("Storing embedding. Current size before insert: " + store.size());
        store.add(new VectorEntry(content, embedding));
        System.out.println("Size after insert: " + store.size());    }

    // Search topK similar chunks
    public List<String> search(List<Double> queryEmbedding, int topK) {
        System.out.println("Searching top " + topK + " vectors...");
        System.out.println("Current vector store size: " + store.size());

        return store.stream()
                .sorted(Comparator.comparingDouble(entry ->
                        -cosineSimilarity(queryEmbedding, entry.getEmbedding())))
                .limit(topK)
                .map(VectorEntry::getContent)
                .toList();
    }

    // Cosine Similarity
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {

        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException("Embedding dimensions do not match");
        }

        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            norm1 += Math.pow(v1.get(i), 2);
            norm2 += Math.pow(v2.get(i), 2);
        }

        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-10);
    }
}
