package com.example.WebSocketSSE.service;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.entity.ChatMessage;
import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.repository.ChatMessageRepository;
import com.example.WebSocketSSE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class ChatService {

    private final ChatMessageRepository chatMessageRepository; // 채팅 메시지 저장/조회 JPA 리포지토리
    private final UserRepository userRepository; // 사용자 정보 조회 JPA 리포지토리

    public Long findUserIdByUsername(String username) { // username으로 사용자 ID 조회
        return userRepository.findByUsername(username) // username으로 사용자 검색
                .map(User::getId) // 존재하면 ID 추출
                .orElseThrow(() -> new RuntimeException("User not found: " + username)); // 없으면 예외 발생
    }

    public ChatMessageDto save(ChatMessageDto dto) { // 채팅 메시지 저장 메서드
        ChatMessage entity = ChatMessage.builder() // 엔티티 생성
                .roomId(dto.getRoomId()) // 채팅방 ID
                .senderId(dto.getSenderId()) // 보낸 사람 ID
                .content(dto.getContent()) // 메시지 내용
                .build();

        ChatMessage saved = chatMessageRepository.save(entity); // DB에 저장

        return ChatMessageDto.builder() // 저장된 데이터로 DTO 생성
                .id(saved.getId()) // 메시지 ID
                .roomId(saved.getRoomId()) // 채팅방 ID
                .senderId(saved.getSenderId()) // 보낸 사람 ID
                .content(saved.getContent()) // 메시지 내용
                .createdAt(saved.getCreatedAt()) // 생성 시간
                .build();
    }
}

