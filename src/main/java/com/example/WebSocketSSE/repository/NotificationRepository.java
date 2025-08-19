package com.example.WebSocketSSE.repository;

import com.example.WebSocketSSE.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByReceiverIdOrderByIdDesc(Long receiverId);
}
