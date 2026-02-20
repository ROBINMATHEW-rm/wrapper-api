package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;
    private final VectorStoreService vectorStoreService;

    public RagController(RagService ragService, VectorStoreService vectorStoreService) {
        this.ragService = ragService;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPdf(@RequestParam("file") MultipartFile file) {
        String documentId = ragService.processPdf(file);
        int chunkCount = ragService.getDocumentChunkCount(documentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "PDF processed successfully");
        response.put("documentId", documentId);
        response.put("filename", file.getOriginalFilename());
        response.put("chunkCount", chunkCount);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestParam("question") String question,
            @RequestParam(value = "documentId", required = false) String documentId,
            @RequestParam(value = "topK", defaultValue = "3") int topK,
            @RequestParam(value = "threshold", required = false) Double threshold
    ) {
        String answer = ragService.askQuestion(question, documentId, topK, threshold);
        
        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("documentId", documentId != null ? documentId : "all");
        response.put("topK", topK);
        response.put("threshold", threshold != null ? threshold : 0.3);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<String> documentIds = ragService.listDocuments();
        
        Map<String, Object> response = new HashMap<>();
        response.put("documents", documentIds);
        response.put("count", documentIds.size());
        response.put("totalChunks", vectorStoreService.getTotalChunkCount());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentInfo(@PathVariable String documentId) {
        int chunkCount = ragService.getDocumentChunkCount(documentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("chunkCount", chunkCount);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String documentId) {
        ragService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of(
                "message", "Document deleted successfully",
                "documentId", documentId
        ));
    }

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
