package com.example.WebSocketSSE.repository;

import com.example.WebSocketSSE.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> { // ChatMessage 엔티티의 JPA 리포지토리
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId); // 특정 채팅방의 메시지를 생성 시간 오름차순으로 조회
}