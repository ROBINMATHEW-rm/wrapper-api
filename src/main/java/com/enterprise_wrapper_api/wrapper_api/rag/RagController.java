package com.enterprise_wrapper_api.wrapper_api.rag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
@Tag(name = "RAG", description = "Retrieval-Augmented Generation — upload PDFs and ask questions")
@SecurityRequirement(name = "bearerAuth")
public class RagController {

    private final RagService ragService;
    private final VectorStoreService vectorStoreService;

    public RagController(RagService ragService, VectorStoreService vectorStoreService) {
        this.ragService = ragService;
        this.vectorStoreService = vectorStoreService;
    }

    @Operation(
        summary = "Upload a PDF",
        description = "Uploads a PDF, extracts text, splits into chunks, generates embeddings via Ollama, and stores in pgvector"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @Parameter(description = "PDF file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        String documentId = ragService.processPdf(file);
        int chunkCount = ragService.getDocumentChunkCount(documentId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "PDF processed successfully");
        response.put("documentId", documentId);
        response.put("filename", file.getOriginalFilename());
        response.put("chunkCount", chunkCount);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Ask a question",
        description = "Embeds the question, retrieves top-K similar chunks from pgvector, and sends context to Groq LLaMA for an answer"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Answer generated"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "403", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @Parameter(description = "Your question", required = true)
            @RequestParam("question") String question,
            @Parameter(description = "Specific document ID to search, or omit to search all documents")
            @RequestParam(value = "documentId", required = false) String documentId,
            @Parameter(description = "Number of top chunks to retrieve (1–20, default 3)")
            @RequestParam(value = "topK", defaultValue = "3") int topK,
            @Parameter(description = "Minimum similarity threshold (0.0–1.0, default 0.3)")
            @RequestParam(value = "threshold", required = false) Double threshold,
            @Parameter(description = "LLM temperature (0.0–1.0). Low = factual, High = creative. Default 0.2")
            @RequestParam(value = "temperature", required = false) Double temperature
    ) {
        String answer = ragService.askQuestion(question, documentId, topK, threshold, temperature);

        double effectiveTemp = temperature != null ? temperature : 0.2;

        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("documentId", documentId != null ? documentId : "all");
        response.put("topK", topK);
        response.put("threshold", threshold != null ? threshold : 0.3);
        response.put("temperature", effectiveTemp);

        if (effectiveTemp > 0.8) {
            response.put("warning", "High temperature (" + effectiveTemp + ") — answers may be creative or less factually accurate. Recommended range for RAG: 0.1–0.3.");
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all documents", description = "Returns all uploaded document IDs and total chunk count")
    @ApiResponse(responseCode = "200", description = "Document list returned")
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<String> documentIds = ragService.listDocuments();

        Map<String, Object> response = new HashMap<>();
        response.put("documents", documentIds);
        response.put("count", documentIds.size());
        response.put("totalChunks", vectorStoreService.getTotalChunkCount());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get document info", description = "Returns chunk count for a specific document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document info returned"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentInfo(
            @Parameter(description = "Document UUID", required = true)
            @PathVariable String documentId) {
        int chunkCount = ragService.getDocumentChunkCount(documentId);

        Map<String, Object> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("chunkCount", chunkCount);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a document", description = "Deletes a document and all its chunks. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document deleted"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @Parameter(description = "Document UUID to delete", required = true)
            @PathVariable String documentId) {
        ragService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of(
                "message", "Document deleted successfully",
                "documentId", documentId
        ));
    }

    @Operation(summary = "Clear all data", description = "Deletes all documents and chunks from the database. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All data cleared"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAll() {
        vectorStoreService.clear();
        return ResponseEntity.ok(Map.of("message", "All data cleared successfully"));
    }

    @Operation(summary = "Health check", description = "Returns service status, document count, and total chunk count")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "RAG Service");
        health.put("totalDocuments", ragService.listDocuments().size());
        health.put("totalChunks", vectorStoreService.getTotalChunkCount());

        return ResponseEntity.ok(health);
    }
}
