package com.homehn.backend.exception;

import com.homehn.backend.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleApp(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện hành động này"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<HashMap<String, String>>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        var errors = new HashMap<String, String>();
        // Java 21: pattern matching instanceof
        ex.getBindingResult().getAllErrors().forEach(e -> {
            if (e instanceof FieldError fe) {
                errors.put(fe.getField(), fe.getDefaultMessage());
            }
        });
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã có lỗi xảy ra: " + ex.getMessage()));
    }
}
