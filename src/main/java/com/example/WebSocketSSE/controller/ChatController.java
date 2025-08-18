package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;  // ★ payload 명시
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    // 클라이언트 → /app/chat/{roomId}/send
    // 구독 경로  → /topic/chat/{roomId}
    @MessageMapping("/chat/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageDto dto,   //  바디를 DTO로 강제 매핑
                     Principal principal) {          // userId는 principal.getName()

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("Unauthenticated STOMP message (missing principal)");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId is required");
        }
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("message content is required");
        }

        // principal.getName()에는 인터셉터에서 넣어준 userId 문자열("2" 등)이 들어있어야 함
        String name = principal.getName();

        // 디버깅에 도움: 혹시라도 name에 JSON이 들어오면 바로 확인 가능
        if (!name.chars().allMatch(Character::isDigit)) {
            log.error("[CHAT] principal.getName() is not numeric. name={}", name);
            throw new IllegalStateException("Principal name is not numeric userId: " + name);
        }

        Long userId = Long.parseLong(name);

        // 서버에서 강제 세팅 (프론트가 보낸 senderId/roomId는 무시)
        dto.setSenderId(userId);
        // 프론트에서 보낸 roomId가 null이 아니면 그대로 사용
        dto.setRoomId(roomId);
        // 채팅 메시지 저장
        var saved = chatService.save(dto);
        // 구독자에게 메시지 전송
        String destination = "/topic/chat/" + saved.getRoomId();
        // SimpMessagingTemplate를 사용하여 메시지 전송
        template.convertAndSend(destination, saved);
        // 디버깅 로그
        log.info("[CHAT] sent to {} by userId={} content={}", destination, userId, saved.getContent());
    }
}
