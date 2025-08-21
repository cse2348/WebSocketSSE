package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    // 클라 전송: /app/chat/{roomId}/send
    // 구독 경로: /topic/chat/{roomId}
    @MessageMapping("/chat/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageDto dto,
                     Principal principal) {
        // 클라이언트가 /app/chat/{roomId}/send로 메시지를 보내면 이 메서드가 호출됨
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("Unauthenticated STOMP message (missing principal)");
        }
        // roomId가 null이거나 dto가 null이거나 content가 비어있으면 예외 발생
        if (roomId == null) {
            throw new IllegalArgumentException("roomId is required");
        }
        // dto가 null이거나 content가 null 또는 비어있으면 예외 발생
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("message content is required");
        }

        // Principal에서 사용자 ID 추출
        String name = principal.getName();

        // principal.getName()이 userId 문자열이라고 가정 (JwtUtil.getAuthentication 설정에 따름)
        if (!name.chars().allMatch(Character::isDigit)) {
            log.error("[CHAT] principal.getName() is not numeric. name={}", name);
            throw new IllegalStateException("Principal name is not numeric userId: " + name);
        }

        Long userId = Long.parseLong(name);

        // 서버에서 강제 세팅 (프론트가 보낸 senderId/roomId 무시)
        dto.setSenderId(userId);
        dto.setRoomId(roomId);

        // DB 저장
        var saved = chatService.save(dto);

        // 구독자에게 브로드캐스트
        String destination = "/topic/chat/" + saved.getRoomId();
        template.convertAndSend(destination, saved);
        // 로그 출력
        log.info("[CHAT] sent to {} by userId={} content={}", destination, userId, saved.getContent());
    }
}
