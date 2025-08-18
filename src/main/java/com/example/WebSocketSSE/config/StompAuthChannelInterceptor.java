package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    // preSend 메서드는 STOMP 메시지가 채널로 전송되기 전에 호출 -> STOMP CONNECT 요청에 대한 JWT 인증을 수행
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // STOMP 메시지 헤더를 StompHeaderAccessor로 래핑
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // STOMP CONNECT 시 인증 수행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Authorization 헤더에서 JWT 토큰 추출
            String token = accessor.getFirstNativeHeader("Authorization");
            // 디버깅용 로그 출력
            System.out.println("[STOMP] Authorization 헤더: " + token);

            if (token == null || !token.startsWith("Bearer ")) {
                // Authorization 헤더가 없거나 형식이 잘못된 경우 예외 발생
                System.out.println("[STOMP] Authorization 헤더 없음/형식 불일치");
                // 예외 발생
                throw new IllegalArgumentException("Missing or invalid Authorization header in STOMP CONNECT");
            }

            token = token.substring(7); // Bearer  제거
            System.out.println("[STOMP] 추출된 토큰: " + token);
            // JWT 토큰 검증 및 Authentication 객체 생성
            Authentication authentication = jwtUtil.getAuthentication(token);
            // 디버깅용 로그 출력
            if (authentication != null) {
                System.out.println("[STOMP] 인증 성공 - 사용자: " + authentication.getName());
                accessor.setUser(authentication); // 세션에 Authentication 심기
            } else {
                System.out.println("[STOMP] 인증 실패 - Authentication null");
                throw new IllegalArgumentException("Invalid JWT token");
            }
        }
        return message;
    }
}
