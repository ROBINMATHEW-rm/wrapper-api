package com.enterprise_wrapper_api.wrapper_api.rag;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TextChunkService {

    /**
     * Splits text into chunks of a fixed number of words.
     *
     * @param text the full text
     * @param chunkSize number of words per chunk
     * @return list of text chunks
     */
    public List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+"); // split by spaces

        for (int i = 0; i < words.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, words.length);
            String chunk = String.join(" ", Arrays.copyOfRange(words, i, end));
            chunks.add(chunk);
        }

        return chunks;
    }
}
