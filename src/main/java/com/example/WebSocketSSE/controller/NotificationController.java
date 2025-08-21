package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.NotificationDto;
import com.example.WebSocketSSE.service.NotificationService;
import com.example.WebSocketSSE.repository.UserRepository;
import com.example.WebSocketSSE.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class NotificationController {

    private final NotificationService notificationService; // 알림 서비스
    private final UserRepository userRepository; // 사용자 조회용 리포지토리

    // 구독 엔드포인트: EventSource로 GET 요청 (SSE)
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // SSE MIME 타입
    public SseEmitter subscribe(Authentication authentication) { // 인증 정보에서 사용자 식별
        Long userId = resolveUserId(authentication)
                .orElseThrow(() -> new RuntimeException("인증 정보에서 userId 확인 실패")); // 인증 누락 방어
        return notificationService.subscribe(userId); // 사용자별 Emitter 등록 및 반환
    }

    // 특정 사용자에게 알림 전송 (서버→클라이언트 푸시 트리거)
    @PostMapping("/notify")
    public NotificationDto notifyToUser(@RequestBody NotificationDto request) { // 요청 바디 유효성 검사
        if (request.getReceiverId() == null) throw new IllegalArgumentException("receiverId 는 필수입니다."); // 수신자 필수
        if (request.getTitle() == null || request.getMessage() == null) throw new IllegalArgumentException("title, message 는 필수입니다."); // 제목/메시지 필수
        return notificationService.notifyToUser(request); // 알림 생성 및 전송
    }

    // 내 알림 수신 내역 조회
    @GetMapping("/history")
    public java.util.List<NotificationDto> myHistory(Authentication authentication) { // 인증 사용자 기준 조회
        Long userId = resolveUserId(authentication)
                .orElseThrow(() -> new RuntimeException("인증 정보에서 userId 확인 실패")); // 인증 누락 방어
        return notificationService.history(userId); // 사용자별 알림 히스토리 반환
    }

    // SecurityContext의 principal 로부터 userId 추론
    private Optional<Long> resolveUserId(Authentication authentication) {
        if (authentication == null) return Optional.empty(); // 비인증 요청 처리
        Object principal = authentication.getPrincipal(); // principal 추출

        if (principal instanceof UserDetails ud) { // UserDetails이면 username 사용
            return userRepository.findByUsername(ud.getUsername()).map(User::getId); // DB에서 id 조회
        }
        if (principal instanceof String s) { // String이면 userId 또는 username 가능
            try {
                return Optional.of(Long.parseLong(s)); // 숫자면 곧장 userId로 사용
            } catch (NumberFormatException ignore) {
                return userRepository.findByUsername(s).map(User::getId); // 숫자 아니면 username으로 조회
            }
        }
        return Optional.empty(); // 그 외 타입은 미지원
    }
}
