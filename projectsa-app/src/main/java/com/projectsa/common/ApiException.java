package com.projectsa.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** An error the page shows to the user (Thai message), optionally per form field (DetectBlank results). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fields;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.fields = fields;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, String> fields() {
        return fields;
    }

    /** 400 with one message per wrong field (narrative alternative flow "แจ้งเตือนที่ช่องที่ผิด"). */
    public static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ครบหรือไม่ถูกต้อง", errors);
        }
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
