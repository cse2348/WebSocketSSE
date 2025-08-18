package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    // 클라이언트 → /app/chat/{roomId}/send 로 전송하면 처리
    // 구독 경로는 /topic/chat/{roomId}
    @MessageMapping("/chat/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     ChatMessageDto dto,
                     @AuthenticationPrincipal String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Unauthenticated STOMP message (missing principal userId)");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId is required");
        }
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("message content is required");
        }

        // sender/room 설정
        dto.setSenderId(Long.parseLong(userId));
        dto.setRoomId(roomId);

        // 저장 후 브로드캐스트
        var saved = chatService.save(dto);
        template.convertAndSend("/topic/chat/" + saved.getRoomId(), saved);
    }
}
