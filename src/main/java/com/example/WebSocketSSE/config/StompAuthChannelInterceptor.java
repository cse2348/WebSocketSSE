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
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                // 1) Authorization 헤더(대/소문자 모두) 조회
                String auth = acc.getFirstNativeHeader("Authorization");
                if (auth == null) auth = acc.getFirstNativeHeader("authorization");

                if (auth == null || auth.isBlank()) {
                    throw new IllegalArgumentException("Missing Authorization header");
                }

                // 2) "Bearer", "Bearer    ", "bearer", 공백 유무 전부 허용
                String token;
                if (auth.regionMatches(true, 0, "Bearer", 0, 6)) {
                    token = auth.substring(6).trim(); // "Bearer" 제거 후 공백 트림
                } else {
                    // 혹시 토큰만 들어오는 경우도 허용
                    token = auth.trim();
                }
                if (token.isEmpty()) throw new IllegalArgumentException("Empty JWT token");

                // 3) 토큰 → Authentication (principal = userId 문자열)
                Authentication authentication = jwtUtil.getAuthentication(token);
                acc.setUser(authentication);

                System.out.println("[STOMP] 인증 성공 - principal=" + authentication.getPrincipal());
            } catch (Exception e) {
                System.out.println("[STOMP] 인증 처리 중 예외: " + e.getMessage());
                throw e;
            }
        }
        return message;
    }
}
