package com.enterprise_wrapper_api.wrapper_api.rag.exception;

public class DocumentNotFoundException extends RagException {
    
    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }
}
