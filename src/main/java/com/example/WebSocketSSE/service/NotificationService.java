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

@Slf4j // 로깅용
@Service // 서비스 빈 등록
@RequiredArgsConstructor // 생성자 주입
public class NotificationService {

    private static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L; // SSE 연결 기본 타임아웃 (30분)

    private final NotificationRepository notificationRepository; // 알림 DB 저장소
    private final SseEmitterRepository emitterRepository; // SSE 연결 관리소

    // SSE 구독 처리
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT); // 30분 타임아웃으로 Emitter 생성
        emitterRepository.add(userId, emitter); // 사용자별 emitter 저장

        try {
            // 연결 직후 클라이언트로 "connected" 이벤트 전송 (연결 확인용)
            emitter.send(SseEmitter.event()
                    .name("connect") // 이벤트 이름
                    .id(String.valueOf(Instant.now().toEpochMilli())) // 이벤트 ID
                    .data("connected")); // 이벤트 데이터
        } catch (IOException e) {
            emitterRepository.remove(userId, emitter); // 전송 실패 시 emitter 제거
            throw new RuntimeException("SSE 초기 전송 실패", e);
        }
        return emitter; // emitter 반환 → 컨트롤러에서 반환하면 연결 유지됨
    }

    // 알림 생성 및 전송
    @Transactional
    public NotificationDto notifyToUser(NotificationDto req) {
        // 1) DB에 알림 저장
        Notification saved = notificationRepository.save(Notification.builder()
                .receiverId(req.getReceiverId()) // 수신자 ID
                .title(req.getTitle())           // 알림 제목
                .message(req.getMessage())       // 알림 메시지
                .read(false)                     // 처음엔 읽지 않음
                .build());

        // 2) 실시간 전송 (해당 userId에 연결된 모든 emitter로 푸시)
        List<SseEmitter> emitters = emitterRepository.get(req.getReceiverId());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification") // 이벤트 이름
                        .id(String.valueOf(saved.getId())) // 이벤트 ID = 알림 ID
                        .data(NotificationDto.builder() // 전송할 DTO 데이터
                                .id(saved.getId())
                                .receiverId(saved.getReceiverId())
                                .title(saved.getTitle())
                                .message(saved.getMessage())
                                .read(saved.isRead())
                                .createdAt(saved.getCreatedAt())
                                .build()));
            } catch (IOException e) {
                log.warn("SSE 전송 실패 -> emitter 제거: {}", e.getMessage());
                emitterRepository.remove(req.getReceiverId(), emitter); // 전송 실패 emitter 제거
            }
        }

        // 저장된 알림을 DTO로 반환
        return NotificationDto.builder()
                .id(saved.getId())
                .receiverId(saved.getReceiverId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .read(saved.isRead())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // 최근 알림 100개 조회
    @Transactional(readOnly = true)
    public List<NotificationDto> history(Long userId) {
        return notificationRepository.findTop100ByReceiverIdOrderByIdDesc(userId) // 최신순 100개
                .stream()
                .map(n -> NotificationDto.builder() // 엔티티 → DTO 변환
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
