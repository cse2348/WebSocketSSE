package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 시 JWT 검증
        if ("CONNECT".equals(accessor.getCommand().name())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);

                // JwtUtil로 검증 및 유저 ID 추출
                Long userId = jwtUtil.validateAndGetUserId(token);

                // Principal 설정
                accessor.setUser((Principal) () -> String.valueOf(userId));
            }
        }
        return message;
    }
}
