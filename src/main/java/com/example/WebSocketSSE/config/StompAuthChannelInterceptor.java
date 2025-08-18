package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                String auth = accessor.getFirstNativeHeader("Authorization");
                if (auth == null || !auth.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Missing or invalid Authorization header");
                }
                String token = auth.substring(7);

                // 토큰 검증 + Authentication 생성(Principal = userId 문자열)
                Authentication authentication = jwtUtil.getAuthentication(token);
                if (authentication == null) {
                    throw new IllegalArgumentException("Invalid JWT token");
                }

                // STOMP 세션에 인증 주입
                accessor.setUser(authentication);
                System.out.println("[STOMP] 인증 성공 - principal=" + authentication.getPrincipal());

            } catch (Exception e) {
                System.out.println("[STOMP] 인증 처리 중 예외: " + e.getMessage());
                throw e;
            }
        }
        return message;
    }
}
