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
    // 읽음 여부 (true: 읽음, false: 안읽음)
    private Boolean read;
    // 생성 시간 (DB 저장 시 자동 설정)
    private LocalDateTime createdAt;
}
