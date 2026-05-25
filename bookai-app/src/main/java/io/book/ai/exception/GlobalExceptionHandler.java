package io.book.ai.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleApiError(RestClientException ex) {
        return ResponseEntity.status(502).body(Map.of("error", "LLM API call failed: " + ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIoError(IOException ex) {
        return ResponseEntity.status(500).body(Map.of("error", "I/O error: " + ex.getMessage()));
    }
}
