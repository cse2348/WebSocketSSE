package com.example.WebSocketSSE.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 자동 증가 전략 (DB IDENTITY 방식 사용)
    private Long id; // 메시지 ID

    @Column(nullable = false) // NULL 불가 컬럼
    private Long roomId; // 채팅방 ID

    @Column(nullable = false) // NULL 불가 컬럼
    private Long senderId; // 보낸 사람 ID

    @Column(nullable = false) // NULL 불가 컬럼
    private String content; // 메시지 내용

    @Column(nullable = false) // NULL 불가 컬럼
    private LocalDateTime createdAt; // 메시지 생성 시간

    @PrePersist // 엔티티가 저장되기 전에 실행되는 메서드
    public void prePersist() {
        this.createdAt = LocalDateTime.now(); // 저장 전 현재 시간으로 생성 시간 설정
    }
}

