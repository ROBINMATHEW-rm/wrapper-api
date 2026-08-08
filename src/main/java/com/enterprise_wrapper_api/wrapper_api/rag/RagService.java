package com.enterprise_wrapper_api.wrapper_api.rag;

import com.enterprise_wrapper_api.wrapper_api.rag.exception.DocumentNotFoundException;
import com.enterprise_wrapper_api.wrapper_api.rag.exception.RagException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RagService {

    private final PdfService pdfService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RetrieverService retrieverService;
    private final LlamaClient llamaClient;

    // Thread pool for parallel embedding generation
    private final ExecutorService embeddingExecutor = Executors.newFixedThreadPool(5);

    public RagService(PdfService pdfService,
                      EmbeddingService embeddingService,
                      VectorStoreService vectorStoreService,
                      RetrieverService retrieverService,
                      LlamaClient llamaClient) {
        this.pdfService = pdfService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.retrieverService = retrieverService;
        this.llamaClient = llamaClient;
    }

    public String processPdf(MultipartFile file) {
        try {
            String documentId = UUID.randomUUID().toString();
            String filename = file.getOriginalFilename();

            vectorStoreService.storeDocumentMetadata(documentId, filename);

            String text = pdfService.extractText(file);
            List<String> chunks = pdfService.chunkText(text);

            if (chunks.isEmpty()) {
                throw new RagException("No chunks generated from PDF");
            }

            System.out.println("Processing " + chunks.size() + " chunks in parallel batches...");

            // Process in batches of 5 to avoid overwhelming Ollama
            int batchSize = 5;
            AtomicInteger processedCount = new AtomicInteger(0);

            for (int batchStart = 0; batchStart < chunks.size(); batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, chunks.size());
                List<String> batch = chunks.subList(batchStart, batchEnd);

                // Submit all chunks in batch concurrently
                List<Future<EmbeddingResult>> futures = new ArrayList<>();
                for (int i = 0; i < batch.size(); i++) {
                    final int chunkIndex = batchStart + i;
                    final String chunk = batch.get(i);
                    futures.add(embeddingExecutor.submit(() -> {
                        List<Double> embedding = embeddingService.generateEmbedding(chunk);
                        return new EmbeddingResult(chunkIndex, chunk, embedding);
                    }));
                }

                // Wait for batch to complete and store results
                for (Future<EmbeddingResult> future : futures) {
                    try {
                        EmbeddingResult result = future.get(60, TimeUnit.SECONDS);
                        vectorStoreService.store(documentId, result.content, result.embedding, result.index);
                        processedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RagException("Embedding interrupted: " + e.getMessage(), e);
                    } catch (ExecutionException e) {
                        throw new RagException("Embedding failed: " + e.getCause().getMessage(), e);
                    } catch (TimeoutException e) {
                        throw new RagException("Embedding timed out", e);
                    }
                }

                System.out.println("Processed " + processedCount.get() + "/" + chunks.size() + " chunks");
            }

            System.out.println("Processed document: " + documentId + " with " + chunks.size() + " chunks");
            return documentId;

        } catch (Exception e) {
            if (e instanceof RagException) throw e;
            throw new RagException("Failed to process PDF: " + e.getMessage(), e);
        }
    }

    // Simple result holder
    private static class EmbeddingResult {
        final int index;
        final String content;
        final List<Double> embedding;

        EmbeddingResult(int index, String content, List<Double> embedding) {
            this.index = index;
            this.content = content;
            this.embedding = embedding;
        }
    }

    public String askQuestion(String question, String documentId, int topK, Double threshold, Double temperature) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        if (topK <= 0 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }
        if (threshold != null && (threshold < 0.0 || threshold > 1.0)) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        if (temperature != null && (temperature < 0.0 || temperature > 1.0)) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
        }

        double temp = temperature != null ? temperature : 0.2;

        if (documentId != null && !vectorStoreService.documentExists(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }

        try {
            List<String> relevantChunks = retrieverService.retrieveRelevantDocs(
                question, topK, documentId, threshold
            );

            if (relevantChunks.isEmpty()) {
                return "No relevant information found in the document(s). The question may not be related to the uploaded content, or the similarity threshold may be too high.";
            }

            String context = String.join("\n\n", relevantChunks);
            String prompt = buildPrompt(question, context);
            return llamaClient.generateAnswer(prompt, temp);

        } catch (Exception e) {
            if (e instanceof RagException || e instanceof IllegalArgumentException) throw e;
            throw new RagException("Failed to generate answer: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String question, String context) {
        return String.format(
            "You are a helpful assistant that answers questions based on the provided context.\n\n" +
            "Context:\n%s\n\n" +
            "Question: %s\n\n" +
            "Instructions:\n" +
            "- Answer the question based ONLY on the information provided in the context above.\n" +
            "- If the context doesn't contain enough information to answer the question, say so.\n" +
            "- Be concise and accurate.\n" +
            "- Do not make up information that is not in the context.\n\n" +
            "Answer:",
            context, question
        );
    }

    /**
     * Streaming version of askQuestion - returns tokens as Flux<String>
     */
    public Flux<String> askQuestionStream(String question, String documentId, int topK, Double threshold, Double temperature) {
        if (question == null || question.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("Question cannot be empty"));
        }

        double temp = temperature != null ? temperature : 0.2;

        if (documentId != null && !vectorStoreService.documentExists(documentId)) {
            return Flux.error(new DocumentNotFoundException(documentId));
        }

        List<String> relevantChunks = retrieverService.retrieveRelevantDocs(question, topK, documentId, threshold);

        if (relevantChunks.isEmpty()) {
            return Flux.just("No relevant information found in the document(s).");
        }

        String context = String.join("\n\n", relevantChunks);
        String prompt = buildPrompt(question, context);

        return llamaClient.generateAnswerStream(prompt, temp);
    }

    public void deleteDocument(String documentId) {
        if (!vectorStoreService.documentExists(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        vectorStoreService.clearDocument(documentId);
    }

    public List<String> listDocuments() {
        return vectorStoreService.getAllDocumentIds();
    }

    public int getDocumentChunkCount(String documentId) {
        if (!vectorStoreService.documentExists(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        return vectorStoreService.getDocumentChunkCount(documentId);
    }
}
