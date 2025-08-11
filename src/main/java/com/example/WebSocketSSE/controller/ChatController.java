package com.example.WebSocketSSE.controller;

import com.example.WebSocket_SSE.dto.ChatMessageDto;
import com.example.WebSocket_SSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(ChatMessageDto dto, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        dto.setSenderId(userId);
        var saved = chatService.save(dto);
        template.convertAndSend("/topic/chat/" + saved.getRoomId(), saved);
    }
}
