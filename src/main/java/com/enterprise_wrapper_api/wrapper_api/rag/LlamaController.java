package com.enterprise_wrapper_api.wrapper_api.rag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rag")
public class LlamaController {

    @Autowired
    private RetrieverService retrieverService;

    @Autowired
    private LlamaClient llamaClient; // your existing wrapper

    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(@RequestBody String query) {

        // Step 1: Retrieve relevant PDF chunks
        List<String> contextDocs = retrieverService.retrieveRelevantDocs(query, 3); // top 3

        // Step 2: Construct RAG prompt
        String prompt = "Use the following context to answer the question:\n" +
                String.join("\n---\n", contextDocs) +
                "\nQuestion: " + query;

        // Step 3: Call LLaMA API
        String answer = llamaClient.generateAnswer(prompt);

        return ResponseEntity.ok(answer);
    }
}
