package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class HistoryController {
    private final ChatService chatService;

    @GetMapping("/history/{roomId}")
    public List<ChatMessageDto> history(@PathVariable String roomId) {
        return chatService.history(roomId);
    }
}

