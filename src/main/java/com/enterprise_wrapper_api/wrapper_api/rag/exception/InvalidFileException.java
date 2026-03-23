package com.enterprise_wrapper_api.wrapper_api.rag.exception;

public class InvalidFileException extends RagException {
    
    public InvalidFileException(String message) {
        super(message);
    }
    
    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
