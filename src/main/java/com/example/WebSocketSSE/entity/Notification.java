package com.example.WebSocketSSE.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter @Setter // Getter/Setter 자동 생성
@NoArgsConstructor @AllArgsConstructor @Builder // 생성자/빌더 자동 생성
@Entity // JPA 엔티티
@Table(name = "notification") // 매핑할 테이블 이름
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK, AUTO_INCREMENT
    private Long id; // 알림 고유 ID

    @Column(nullable = false)
    private Long receiverId; // 수신자 (User.id)

    @Column(nullable = false, length = 120)
    private String title; // 알림 제목

    @Column(nullable = false, length = 1000)
    private String message; // 알림 본문

    @Builder.Default // 빌더 생성 시 기본값 지정
    @Column(name = "is_read", nullable = false) // 예약어(read) 대신 is_read 컬럼 사용
    private boolean read = false; // 읽음 여부 (기본 false)

    @CreationTimestamp // 생성 시 자동 시간 기록
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시각
}
