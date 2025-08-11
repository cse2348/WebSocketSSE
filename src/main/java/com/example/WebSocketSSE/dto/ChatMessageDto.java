package com.example.WebSocketSSE.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ChatMessageDto {
    private Long id;
    private String roomId;
    private Long senderId;
    private String content;
    private Instant createdAt;
}
