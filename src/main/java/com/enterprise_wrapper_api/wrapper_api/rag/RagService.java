package com.enterprise_wrapper_api.wrapper_api.rag;

import com.enterprise_wrapper_api.wrapper_api.rag.exception.DocumentNotFoundException;
import com.enterprise_wrapper_api.wrapper_api.rag.exception.RagException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private final PdfService pdfService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RetrieverService retrieverService;
    private final LlamaClient llamaClient;

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

            int chunkIndex = 0;
            for (String chunk : chunks) {
                List<Double> embedding = embeddingService.generateEmbedding(chunk);
                vectorStoreService.store(documentId, chunk, embedding, chunkIndex++);
            }

            System.out.println("Processed document: " + documentId + " with " + chunks.size() + " chunks");
            return documentId;
        } catch (Exception e) {
            if (e instanceof RagException) {
                throw e;
            }
            throw new RagException("Failed to process PDF: " + e.getMessage(), e);
        }
    }

    public String askQuestion(String question, String documentId, int topK, Double threshold) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }

        if (topK <= 0 || topK > 20) {
            throw new IllegalArgumentException("topK must be between 1 and 20");
        }

        if (threshold != null && (threshold < 0.0 || threshold > 1.0)) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }

        // Validate document exists if specified
        if (documentId != null && !vectorStoreService.documentExists(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }

        try {
            // Retrieve relevant chunks with optional threshold
            List<String> relevantChunks = retrieverService.retrieveRelevantDocs(
                question, topK, documentId, threshold
            );

            if (relevantChunks.isEmpty()) {
                return "No relevant information found in the document(s). The question may not be related to the uploaded content, or the similarity threshold may be too high.";
            }

            // Build context from retrieved chunks
            String context = String.join("\n\n", relevantChunks);

            // Build prompt for LLM
            String prompt = buildPrompt(question, context);

            // Generate answer using LLM
            return llamaClient.generateAnswer(prompt);
        } catch (Exception e) {
            if (e instanceof RagException || e instanceof IllegalArgumentException) {
                throw e;
            }
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
            context,
            question
        );
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
