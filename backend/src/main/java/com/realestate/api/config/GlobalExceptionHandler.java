package com.realestate.api.config;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tools.jackson.core.JacksonException;

/**
 * Turns validation failures and bad input into a small {"message": "..."}
 * body with a plain-English sentence. Technical detail (parser errors, class
 * names) goes to the server log only - never to the browser.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Field names that read badly when auto-converted from camelCase. */
    private static final Map<String, String> FIELD_LABELS = Map.of("slotTime", "Visit time");

    /**
     * "must not be blank" on field "title" -> "Title must not be blank".
     * A message that already starts with a capital letter is a full sentence
     * (e.g. "Password must be at least 6 characters") and is used as it is.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(
                                err -> {
                                    String text = err.getDefaultMessage();
                                    if (text == null) {
                                        return label(err.getField()) + " is not valid";
                                    }
                                    return Character.isUpperCase(text.charAt(0))
                                            ? text
                                            : label(err.getField()) + " " + text;
                                })
                        .orElse("Please check what you entered and try again.");
        return badRequest(message);
    }

    /** Malformed JSON, or a value of the wrong type/format (e.g. a date with no time zone). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        String message = "We couldn't understand that request. Please check what you entered and try again.";
        if (ex.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
            String field = jackson.getPath().getLast().getPropertyName();
            if (field != null) {
                message = label(field) + " is not valid. Please check it and try again.";
            }
        }
        return badRequest(message);
    }

    /** A URL value that can't become the expected type, e.g. ?type=HOUSE or ?bedrooms=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Bad value for '{}': {}", ex.getName(), ex.getMessage());
        return badRequest(label(ex.getName()) + " is not valid. Please check it and try again.");
    }

    /**
     * A file (or the whole request) is bigger than spring.servlet.multipart allows. Caught
     * here rather than in the upload code, because Spring rejects an oversized upload before
     * the controller method - and its own per-file 3MB check - ever runs.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return badRequest("That upload is too large. Each photo must be 3MB or smaller.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    private static String label(String field) {
        String override = FIELD_LABELS.get(field);
        if (override != null) {
            return override;
        }
        String words = field.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
