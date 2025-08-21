package com.example.WebSocketSSE.repository;

import com.example.WebSocketSSE.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
// NotificationRepository는 알림(Notification) 엔티티에 대한 데이터베이스 작업을 수행하는 JPA 리포지토리
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 사용자에게 전송된 알림을 ID 내림차순으로 최대 100개 조회
    List<Notification> findTop100ByReceiverIdOrderByIdDesc(Long receiverId);
}
