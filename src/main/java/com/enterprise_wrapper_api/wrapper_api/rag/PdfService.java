package com.enterprise_wrapper_api.wrapper_api.rag;

import com.enterprise_wrapper_api.wrapper_api.rag.exception.InvalidFileException;
import com.enterprise_wrapper_api.wrapper_api.rag.exception.RagException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfService {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 200;
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public String extractText(MultipartFile file) {
        validateFile(file);
        
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            if (document.getNumberOfPages() == 0) {
                throw new InvalidFileException("PDF file is empty");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                throw new InvalidFileException("PDF contains no extractable text");
            }
            
            return text;

        } catch (InvalidFileException e) {
            throw e;
        } catch (Exception e) {
            throw new RagException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds maximum allowed size of 100MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF files are supported");
        }
    }

    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Invalid chunk size or overlap parameters");
        }

        // Clean up text
        text = text.replaceAll("\\s+", " ").trim();

        List<String> chunks = new ArrayList<>();
        
        // Split into sentences for semantic chunking
        List<String> sentences = splitIntoSentences(text);
        
        if (sentences.isEmpty()) {
            return chunks;
        }

        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceLength = sentence.length();

            // If adding this sentence exceeds chunk size and we have content
            if (currentLength + sentenceLength > chunkSize && currentLength > 0) {
                // Save current chunk
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap
                currentChunk = new StringBuilder();
                currentLength = 0;

                // Add sentences for overlap (go back and include previous sentences)
                int overlapLength = 0;
                int backtrack = i - 1;
                List<String> overlapSentences = new ArrayList<>();

                while (backtrack >= 0 && overlapLength < overlap) {
                    String prevSentence = sentences.get(backtrack);
                    if (overlapLength + prevSentence.length() <= overlap) {
                        overlapSentences.add(0, prevSentence);
                        overlapLength += prevSentence.length();
                        backtrack--;
                    } else {
                        break;
                    }
                }

                // Add overlap sentences to new chunk
                for (String overlapSent : overlapSentences) {
                    currentChunk.append(overlapSent).append(" ");
                    currentLength += overlapSent.length() + 1;
                }
            }

            // Add current sentence
            currentChunk.append(sentence).append(" ");
            currentLength += sentenceLength + 1;
        }

        // Add the last chunk if it has content
        if (currentLength > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        System.out.println("Created " + chunks.size() + " semantic chunks");
        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // Pattern to split on sentence boundaries while preserving abbreviations
        // Matches: . ! ? followed by space and capital letter, or end of string
        Pattern sentencePattern = Pattern.compile(
            "([^.!?]+[.!?]+)(?=\\s+[A-Z]|\\s*$)"
        );
        
        Matcher matcher = sentencePattern.matcher(text);
        
        while (matcher.find()) {
            String sentence = matcher.group(1).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        
        // Fallback: if no sentences found, split by periods
        if (sentences.isEmpty()) {
            String[] parts = text.split("\\.");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed + ".");
                }
            }
        }
        
        return sentences;
    }
}
