package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.ChatMessageDto;
import com.example.WebSocketSSE.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // REST API 컨트롤러 (JSON 응답)
@RequestMapping("/chat/history") // 기본 URL 경로 /chat/history
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class HistoryController {

    private final ChatMessageRepository chatMessageRepository; // 채팅 메시지 조회용 JPA 리포지토리

    @GetMapping("/{roomId}") // GET /chat/history/{roomId} 요청 처리
    public List<ChatMessageDto> getHistory(@PathVariable Long roomId) { // URL 경로에서 roomId 추출
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId) // 해당 채팅방의 메시지를 생성 시간 오름차순으로 조회
                .stream()
                .map(msg -> ChatMessageDto.builder() // 엔티티를 DTO로 변환
                        .id(msg.getId()) // 메시지 ID
                        .roomId(msg.getRoomId()) // 채팅방 ID
                        .senderId(msg.getSenderId()) // 보낸 사람 ID
                        .content(msg.getContent()) // 메시지 내용
                        .createdAt(msg.getCreatedAt()) // 메시지 생성 시간
                        .build())
                .toList(); // 변환된 DTO 리스트 반환
    }
}


