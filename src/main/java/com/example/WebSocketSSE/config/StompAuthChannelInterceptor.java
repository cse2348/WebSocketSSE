package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

        // 초기 CONNECT 명령에 대해서만 인증 확인
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                String authHeader = acc.getFirstNativeHeader("Authorization");
                if (authHeader == null) {
                    authHeader = acc.getFirstNativeHeader("authorization");
                }

                if (authHeader != null && !authHeader.isBlank()) {
                    // JWT 토큰에서 인증 정보 추출
                    Authentication authentication = jwtUtil.getAuthentication(authHeader);
                    // 인증 정보가 유효한 경우 SecurityContext에 설정
                    acc.setUser(authentication);
                    log.info("[WS] CONNECT 인증 성공. userId={}", authentication.getName());
                } else {
                    // 익명 연결 허용
                    log.info("[WS] CONNECT 익명 연결.");
                }
            } catch (Exception e) {
                // 로그를 더 자세히 찍기 (메시지 + 예외 클래스명 + 전체 스택)
                log.warn("[WS] CONNECT 인증 실패: {} ({})", e.getMessage(), e.getClass().getName(), e);
                // 연결 거부하기 위해 예외를 다시 던짐
                throw new IllegalArgumentException("인증 실패: " + e.getMessage(), e);
            }
        }
        return message;
    }
}
