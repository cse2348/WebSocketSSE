package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller // 웹소켓/STOMP 메시지 처리를 담당하는 컨트롤러
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class ChatController {
    private final SimpMessagingTemplate template; // 메시지를 특정 구독자들에게 전송하는 유틸
    private final ChatService chatService; // 채팅 메시지 저장/조회 서비스

    @MessageMapping("/chat.send") // 클라이언트에서 "/app/chat.send" 경로로 전송한 메시지 처리
    public void send(ChatMessageDto dto, Principal principal) { // Principal로 인증된 사용자 정보 받음
        Long userId = Long.valueOf(principal.getName()); // Principal에서 사용자 ID 추출
        dto.setSenderId(userId); // 메시지 보낸 사람 ID 설정
        var saved = chatService.save(dto); // 채팅 메시지 저장
        template.convertAndSend("/topic/chat/" + saved.getRoomId(), saved); // 해당 채팅방 구독자들에게 메시지 전송
    }
}

