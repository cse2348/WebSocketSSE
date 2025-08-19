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

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 구독 (브라우저/콘솔 EventSource에서 GET)
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        Long userId = resolveUserId(authentication)
                .orElseThrow(() -> new RuntimeException("인증 정보에서 userId 확인 실패"));
        return notificationService.subscribe(userId);
    }

    // 특정 사용자에게 알림 전송 (POST)
    @PostMapping("/notify")
    public NotificationDto notifyToUser(@RequestBody NotificationDto request) {
        // request.receiverId 에게 전송
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("receiverId 는 필수입니다.");
        }
        if (request.getTitle() == null || request.getMessage() == null) {
            throw new IllegalArgumentException("title, message 는 필수입니다.");
        }
        return notificationService.notifyToUser(request);
    }

    // 내 알림 목록 조회
    @GetMapping("/history")
    public java.util.List<NotificationDto> myHistory(Authentication authentication) {
        Long userId = resolveUserId(authentication)
                .orElseThrow(() -> new RuntimeException("인증 정보에서 userId 확인 실패"));
        return notificationService.history(userId);
    }

    /**
     * SecurityContext 의 principal 로부터 userId 추론
     * - principal 이 숫자 문자열이면 그대로 사용
     * - principal 이 username 인 경우 DB 에서 findByUsername 으로 id 조회
     */
    private Optional<Long> resolveUserId(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails ud) {
            // username → DB 로 id 조회
            return userRepository.findByUsername(ud.getUsername()).map(User::getId);
        }
        if (principal instanceof String s) {
            try {
                // 토큰에 userId 를 principal 로 싣는 구현일 수도 있음
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException ignore) {
                // username 일 수 있으니 DB 조회 시도
                return userRepository.findByUsername(s).map(User::getId);
            }
        }
        return Optional.empty();
    }
}
