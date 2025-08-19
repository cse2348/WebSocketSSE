package com.example.WebSocketSSE.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long receiverId;      // 전송 대상 (POST /sse/notify 요청에서 필수)
    private String title;
    private String message;

    // 응답/전달용
    private Long id;
    private Boolean read;
    private LocalDateTime createdAt;
}
