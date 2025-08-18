package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;                 // ★ 바디 역직렬화용
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ★ STOMP Principal 주입
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template; // 특정 주제(/topic/...)로 메시지 브로드캐스트
    private final ChatService chatService;        // 메시지 저장/조회 등 비즈니스 로직

    /**
     * 클라이언트에서 "/app/chat/{roomId}/send"로 전송한 STOMP 메시지를 처리한다.
     * - 구독 경로는 "/topic/chat/{roomId}"
     * - 인터셉터에서 principal에 userId 문자열을 심어둔 전제(@AuthenticationPrincipal String userId)
     *
     * 예) 클라이언트 전송
     * stompClient.send("/app/chat/1/send", {}, JSON.stringify({ "content": "안녕" }));
     *
     * 주의:
     * - ChatMessageDto는 반드시 기본 생성자 + getter/setter가 있어야 역직렬화가 된다.
     *   (예: Lombok @Data, @NoArgsConstructor 사용)
     */
    @MessageMapping("/chat/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageDto dto,            // ★ JSON 바디 → DTO로 역직렬화
                     @AuthenticationPrincipal String userId)  // ★ principal에 넣어둔 userId 문자열
    {
        // --- 방어 로직 (디버깅 시 원인 파악 용이) ---
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Unauthenticated STOMP message (missing principal userId)");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("roomId is required");
        }
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("message content is required");
        }

        // --- 서버에서 강제 세팅(프론트가 보낸 값 덮어씀) ---
        dto.setSenderId(Long.parseLong(userId)); // principal → Long 변환
        dto.setRoomId(roomId);                   // 경로 변수에서 받은 roomId 사용

        // --- 저장(영속화) ---
        var saved = chatService.save(dto);

        // --- 브로드캐스트: 이 방을 구독 중인 모두에게 전송 ---
        String destination = "/topic/chat/" + saved.getRoomId();
        template.convertAndSend(destination, saved);

        // --- 로깅 ---
        log.info("[CHAT] roomId={}, senderId={}, content={}",
                saved.getRoomId(), saved.getSenderId(), saved.getContent());
    }
}
