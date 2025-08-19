package com.example.WebSocketSSE.service;

import com.example.WebSocketSSE.dto.NotificationDto;
import com.example.WebSocketSSE.entity.Notification;
import com.example.WebSocketSSE.repository.NotificationRepository;
import com.example.WebSocketSSE.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // 30분 (필요시 조정)
    private static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L;

    private final NotificationRepository notificationRepository;
    private final SseEmitterRepository emitterRepository;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.add(userId, emitter);

        // 연결 확인용 더미 이벤트
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .id(String.valueOf(Instant.now().toEpochMilli()))
                    .data("connected"));
        } catch (IOException e) {
            emitterRepository.remove(userId, emitter);
            throw new RuntimeException("SSE 초기 전송 실패", e);
        }
        return emitter;
    }

    @Transactional
    public NotificationDto notifyToUser(NotificationDto req) {
        // 1) DB 저장
        Notification saved = notificationRepository.save(Notification.builder()
                .receiverId(req.getReceiverId())
                .title(req.getTitle())
                .message(req.getMessage())
                .read(false)
                .build());

        // 2) 실시간 전송 (열려있는 모든 emitter에 push)
        List<SseEmitter> emitters = emitterRepository.get(req.getReceiverId());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(String.valueOf(saved.getId()))
                        .data(NotificationDto.builder()
                                .id(saved.getId())
                                .receiverId(saved.getReceiverId())
                                .title(saved.getTitle())
                                .message(saved.getMessage())
                                .read(saved.isRead())
                                .createdAt(saved.getCreatedAt())
                                .build()));
            } catch (IOException e) {
                log.warn("SSE 전송 실패 -> emitter 제거: {}", e.getMessage());
                emitterRepository.remove(req.getReceiverId(), emitter);
            }
        }

        return NotificationDto.builder()
                .id(saved.getId())
                .receiverId(saved.getReceiverId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .read(saved.isRead())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> history(Long userId) {
        return notificationRepository.findTop100ByReceiverIdOrderByIdDesc(userId)
                .stream()
                .map(n -> NotificationDto.builder()
                        .id(n.getId())
                        .receiverId(n.getReceiverId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
