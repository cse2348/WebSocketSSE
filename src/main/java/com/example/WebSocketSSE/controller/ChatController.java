package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller // 웹소켓/STOMP 메시지 처리를 담당하는 컨트롤러
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class ChatController {
    private final SimpMessagingTemplate template; // 메시지를 특정 구독자들에게 전송하는 유틸
    private final ChatService chatService; // 채팅 메시지 저장/조회 서비스

    @MessageMapping("/chat/{roomId}/send") // 클라이언트에서 "/app/chat/{roomId}/send" 경로로 전송한 메시지 처리
    public void send(@DestinationVariable Long roomId, ChatMessageDto dto, Principal principal) {
        if (principal == null) { // 인증 누락 방어(디버깅 용이)
            throw new IllegalStateException("Unauthenticated STOMP message (missing Principal)");
        }

        Long userId;
        try {
            // Principal이 userId(Long)일 때
            userId = Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            // Principal이 username(String)일 때 → DB에서 userId 조회
            userId = chatService.findUserIdByUsername(principal.getName());
        }
        dto.setSenderId(userId); // 메시지 보낸 사람 ID 설정
        dto.setRoomId(roomId);   // 메시지 대상 채팅방 설정

        if (dto.getRoomId() == null) { // roomId 누락 방어
            throw new IllegalArgumentException("roomId is required in message body");
        }

        var saved = chatService.save(dto); // 채팅 메시지 저장
        template.convertAndSend("/topic/chat/" + saved.getRoomId(), saved); // 해당 채팅방 구독자들에게 메시지 전송
    }
}
