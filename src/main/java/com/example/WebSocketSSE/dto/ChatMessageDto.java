package com.example.WebSocketSSE.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    // 메시지 ID
    private Long id;
    // 채팅방 ID
    private Long roomId;
    // 보낸 사람 ID
    private Long senderId;
    // 메시지 내용
    private String content;
    // 메시지 생성 시간
    private LocalDateTime createdAt;
}