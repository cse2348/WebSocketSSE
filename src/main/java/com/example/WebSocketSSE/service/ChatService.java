package com.example.WebSocketSSE.service;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.entity.ChatMessage;
import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.repository.ChatMessageRepository;
import com.example.WebSocketSSE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public Long findUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public ChatMessageDto save(ChatMessageDto dto) {
        ChatMessage entity = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .senderId(dto.getSenderId())
                .content(dto.getContent())
                .build();

        ChatMessage saved = chatMessageRepository.save(entity);

        return ChatMessageDto.builder()
                .id(saved.getId())
                .roomId(saved.getRoomId())
                .senderId(saved.getSenderId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
