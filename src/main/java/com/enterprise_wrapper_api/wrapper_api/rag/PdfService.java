package com.enterprise_wrapper_api.wrapper_api.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    // Extract text from uploaded PDF
    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF", e);
        }
    }

    // Chunk text for RAG
    public List<String> chunkText(String text) {

        int chunkSize = 800;
        int overlap = 100;

        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += (chunkSize - overlap)) {
            int end = Math.min(text.length(), i + chunkSize);
            chunks.add(text.substring(i, end));
        }

        return chunks;
    }
}
