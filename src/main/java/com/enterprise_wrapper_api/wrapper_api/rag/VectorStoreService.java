package com.enterprise_wrapper_api.wrapper_api.rag;

import com.enterprise_wrapper_api.wrapper_api.rag.entity.Document;
import com.enterprise_wrapper_api.wrapper_api.rag.entity.VectorChunk;
import com.enterprise_wrapper_api.wrapper_api.rag.repository.DocumentRepository;
import com.enterprise_wrapper_api.wrapper_api.rag.repository.VectorChunkRepository;
import com.pgvector.PGvector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;
    private final DocumentRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;

    public VectorStoreService(DocumentRepository documentRepository,
                              VectorChunkRepository vectorChunkRepository) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
    }

    private static class ScoredResult {
        private final String content;
        private final double score;

        public ScoredResult(String content, double score) {
            this.content = content;
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public double getScore() {
            return score;
        }
    }

    @Transactional
    public void clear() {
        vectorChunkRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Transactional
    public void clearDocument(String documentId) {
        vectorChunkRepository.deleteByDocument_DocumentId(documentId);
        documentRepository.deleteByDocumentId(documentId);
    }

    @Transactional
    public void storeDocumentMetadata(String documentId, String filename) {
        Document document = new Document();
        document.setDocumentId(documentId);
        document.setFilename(filename);
        document.setChunkCount(0);
        documentRepository.save(document);
    }

    @Transactional
    public void store(String documentId, String content, List<Double> embedding, int chunkIndex) {
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        VectorChunk chunk = new VectorChunk();
        chunk.setDocument(document);
        chunk.setContent(content);
        chunk.setEmbedding(new PGvector(convertToFloatArray(embedding)));
        chunk.setChunkIndex(chunkIndex);

        vectorChunkRepository.save(chunk);

        // Update chunk count
        document.setChunkCount(document.getChunkCount() + 1);
        documentRepository.save(document);

        System.out.println("Stored embedding for doc: " + documentId + ", chunk: " + chunkIndex);
    }

    public List<String> search(List<Double> queryEmbedding, int topK) {
        return search(queryEmbedding, topK, null, DEFAULT_SIMILARITY_THRESHOLD);
    }

    public List<String> search(List<Double> queryEmbedding, int topK, String documentId) {
        return search(queryEmbedding, topK, documentId, DEFAULT_SIMILARITY_THRESHOLD);
    }

    public List<String> search(List<Double> queryEmbedding, int topK, String documentId, double threshold) {
        System.out.println("Searching top " + topK + " vectors with threshold: " + threshold);

        // Convert embedding to pgvector format string
        String vectorString = convertToVectorString(queryEmbedding);

        List<VectorChunk> chunks;
        if (documentId != null) {
            chunks = vectorChunkRepository.findNearestNeighborsByDocument(vectorString, documentId, topK * 2);
        } else {
            chunks = vectorChunkRepository.findNearestNeighbors(vectorString, topK * 2);
        }

        System.out.println("Retrieved " + chunks.size() + " candidates from database");

        // Calculate actual similarity scores and filter by threshold
        List<ScoredResult> scoredResults = chunks.stream()
                .map(chunk -> {
                    List<Double> chunkEmbedding = convertToDoubleList(chunk.getEmbedding());
                    double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                    return new ScoredResult(chunk.getContent(), similarity);
                })
                .filter(result -> result.getScore() >= threshold)
                .sorted(Comparator.comparingDouble(ScoredResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        System.out.println("Found " + scoredResults.size() + " results above threshold");

        // Log similarity scores for debugging
        for (int i = 0; i < scoredResults.size(); i++) {
            System.out.println("Result " + (i + 1) + " - Similarity: " +
                    String.format("%.4f", scoredResults.get(i).getScore()));
        }

        return scoredResults.stream()
                .map(ScoredResult::getContent)
                .collect(Collectors.toList());
    }

    public List<String> getAllDocumentIds() {
        return documentRepository.findAll().stream()
                .map(Document::getDocumentId)
                .collect(Collectors.toList());
    }

    public int getDocumentChunkCount(String documentId) {
        return vectorChunkRepository.countByDocumentId(documentId);
    }

    public boolean documentExists(String documentId) {
        return documentRepository.existsByDocumentId(documentId);
    }

    public int getTotalChunkCount() {
        return (int) vectorChunkRepository.count();
    }

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

    private float[] convertToFloatArray(List<Double> embedding) {
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    private List<Double> convertToDoubleList(PGvector pgvector) {
        float[] floats = pgvector.toArray();
        List<Double> result = new ArrayList<>(floats.length);
        for (float f : floats) {
            result.add((double) f);
        }
        return result;
    }

    private String convertToVectorString(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
