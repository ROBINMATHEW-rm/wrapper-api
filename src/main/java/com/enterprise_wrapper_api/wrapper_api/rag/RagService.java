package com.enterprise_wrapper_api.wrapper_api.rag;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class RagService {

    private final PdfService pdfService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public RagService(PdfService pdfService,
                      EmbeddingService embeddingService,
                      VectorStoreService vectorStoreService) {
        this.pdfService = pdfService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }
    public void processPdf(MultipartFile file) {
        vectorStoreService.clear(); // reset before loading new PDF
        String text = pdfService.extractText(file);
        List<String> chunks = pdfService.chunkText(text);

        for (String chunk : chunks) {

            List<Double> embeddingArray = embeddingService.generateEmbedding(chunk);

            List<Double> embedding = new java.util.ArrayList<>();
            for (Double value : embeddingArray) {
                embedding.add((double) value);
            }

            vectorStoreService.store(chunk, embedding);
        }
    }
}
