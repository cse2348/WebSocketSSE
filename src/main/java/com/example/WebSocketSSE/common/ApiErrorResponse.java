package com.example.WebSocketSSE.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private int status;                 // HTTP status code
    private String error;               // e.g. "Unauthorized"
    private String message;             // human readable message
    private String path;                // request path
    private LocalDateTime timestamp;    // server time
    private Map<String, String> details; // field errors, etc.

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return ApiErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
