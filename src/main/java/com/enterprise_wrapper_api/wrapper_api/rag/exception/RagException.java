package com.enterprise_wrapper_api.wrapper_api.rag.exception;

public class RagException extends RuntimeException {
    
    public RagException(String message) {
        super(message);
    }
    
    public RagException(String message, Throwable cause) {
        super(message, cause);
    }
}
