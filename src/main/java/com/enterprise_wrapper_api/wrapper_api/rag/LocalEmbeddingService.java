package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple local embedding service using TF-IDF approach
 * This is a basic implementation for testing without external API calls
 */
@Service
public class LocalEmbeddingService {

    private static final int EMBEDDING_SIZE = 384; // Standard size for sentence transformers
    private final Random random = new Random(42); // Fixed seed for consistency

    /**
     * Generate a simple embedding vector for text
     * Uses a combination of character-based hashing and word-based features
     */
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return generateZeroVector();
        }

        text = text.toLowerCase().trim();
        
        // Create embedding vector
        double[] embedding = new double[EMBEDDING_SIZE];
        
        // 1. Character-level features (first 128 dimensions)
        for (int i = 0; i < Math.min(128, EMBEDDING_SIZE); i++) {
            int charIndex = i % text.length();
            char c = text.charAt(charIndex);
            embedding[i] = (double) c / 255.0; // Normalize to 0-1
        }
        
        // 2. Word-level features (next 128 dimensions)
        String[] words = text.split("\\s+");
        for (int i = 128; i < Math.min(256, EMBEDDING_SIZE) && i < 128 + words.length; i++) {
            String word = words[i - 128];
            embedding[i] = (double) word.hashCode() / Integer.MAX_VALUE;
        }
        
        // 3. Statistical features (remaining dimensions)
        for (int i = 256; i < EMBEDDING_SIZE; i++) {
            int index = i - 256;
            if (index == 0) {
                embedding[i] = (double) text.length() / 1000.0; // Text length feature
            } else if (index == 1) {
                embedding[i] = (double) words.length / 100.0; // Word count feature
            } else if (index == 2) {
                embedding[i] = countVowels(text) / (double) text.length(); // Vowel ratio
            } else {
                // Fill remaining with deterministic pseudo-random values based on text
                Random textRandom = new Random(text.hashCode() + index);
                embedding[i] = textRandom.nextGaussian() * 0.1;
            }
        }
        
        // Normalize the vector
        double norm = 0.0;
        for (double v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        // Convert to List<Double>
        List<Double> result = new ArrayList<>(EMBEDDING_SIZE);
        for (double v : embedding) {
            result.add(v);
        }
        
        return result;
    }

    private List<Double> generateZeroVector() {
        List<Double> zero = new ArrayList<>(EMBEDDING_SIZE);
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            zero.add(0.0);
        }
        return zero;
    }

    private int countVowels(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if ("aeiou".indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
    }
}
