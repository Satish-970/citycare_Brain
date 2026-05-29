package org.citycare.facilityservice.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.citycare.facilityservice.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handles @Valid failures (Regex, Email, Size, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed for one or more fields")
                .data(errors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Handles Resource Not Found Errors
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(
                ApiResponse.error(ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    // 3. Handles JSON parsing errors (Specifically Enum mismatches like DOCTORR)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleJsonErrors(HttpMessageNotReadableException ex) {
        String message = "Invalid input format";

        // Logic to extract Enum values if the cause is an Invalid Format
        if (ex.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType().isEnum()) {
                String fieldName = ife.getPath().get(0).getFieldName();
                String invalidValue = ife.getValue().toString();
                String allowedValues = Arrays.toString(ife.getTargetType().getEnumConstants());

                message = String.format("Invalid value '%s' for field '%s'. Allowed values are: %s",
                        invalidValue, fieldName, allowedValues);
            }
        }

        return new ResponseEntity<>(
                ApiResponse.<String>builder()
                        .success(false)
                        .message(message)
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: insufficient permissions"));
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<String>> handleFeignException(feign.FeignException ex) {
        // This catches errors coming from the AuthService (like 400, 401, 500)
        return new ResponseEntity<>(
                ApiResponse.error("Auth Service Error: " + ex.getMessage()),
                HttpStatus.valueOf(ex.status() > 0 ? ex.status() : 500)
        );
    }
}