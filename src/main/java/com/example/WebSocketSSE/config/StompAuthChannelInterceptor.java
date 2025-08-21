package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

        // STOMP CONNECT 명령일 때만 인증을 처리합니다.
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                log.debug("[WS] CONNECT 시도. headers={}", acc.toNativeHeaderMap());

                // 1. 헤더에서 JWT 토큰을 추출합니다.
                String token = resolveToken(acc);

                // 2. 토큰으로 인증 정보를 생성합니다.
                Authentication authentication = jwtUtil.getAuthentication(token);

                // 3. Spring Security 컨텍스트에 인증 정보를 저장합니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 4. STOMP 세션에 인증 정보를 연결(set)합니다.
                acc.setUser(authentication);

                log.info("[WS] CONNECT 인증 성공. user={}", authentication.getName());

            } catch (Exception e) {
                // 인증 과정에서 예외 발생 시, 로그를 남기고 클라이언트에게 에러 메시지를 전달합니다.
                log.error("[WS] CONNECT 인증 실패: {}", e.getMessage());
                // MessageDeliveryException을 사용하면 클라이언트의 STOMP ERROR 프레임 message 헤더에 상세 사유가 찍힙니다.
                throw new MessageDeliveryException("CONNECT 인증 실패: " + e.getMessage());
            }
        }
        return message;
    }

    /**
     * STOMP 헤더에서 JWT 토큰을 추출하는 헬퍼 메서드입니다.
     * 다양한 헤더 키("Authorization", "access_token" 등)를 지원하고, "Bearer " 접두사를 처리합니다.
     */
    private String resolveToken(StompHeaderAccessor accessor) {
        // 여러 일반적인 헤더 키를 순서대로 확인합니다.
        String header = first(accessor, "Authorization");
        if (header == null) header = first(accessor, "authorization");
        if (header == null) header = first(accessor, "access_token");

        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Authorization 헤더를 찾을 수 없습니다.");
        }

        // "Bearer " 접두사가 있으면 제거합니다.
        String token = header.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        if (token.isEmpty()) {
            throw new IllegalArgumentException("토큰이 비어있습니다.");
        }

        return token;
    }

    // 헤더 맵에서 특정 키의 첫 번째 값을 안전하게 가져옵니다.
    private String first(StompHeaderAccessor acc, String key) {
        String value = acc.getFirstNativeHeader(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}