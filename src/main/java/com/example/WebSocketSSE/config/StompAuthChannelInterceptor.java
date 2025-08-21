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

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                String token = resolveToken(acc);
                Authentication authentication = jwtUtil.getAuthentication(token);

                // SecurityContext + STOMP 세션에 주입
                SecurityContextHolder.getContext().setAuthentication(authentication);
                acc.setUser(authentication);

                log.info("[WS] CONNECT 인증 성공. user={}", authentication.getName());
            } catch (Exception e) {
                log.error("[WS] CONNECT 인증 실패.", e);
                throw new IllegalArgumentException("인증에 실패했습니다: " + e.getMessage());
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String h = accessor.getFirstNativeHeader("Authorization");
        if (h == null) h = accessor.getFirstNativeHeader("authorization");
        if (h == null) h = accessor.getFirstNativeHeader("AUTHORIZATION");
        if (h == null) h = accessor.getFirstNativeHeader("access_token");
        if (h == null || h.isBlank()) throw new IllegalArgumentException("Authorization header missing");

        String v = h.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) v = v.substring(7).trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Empty token");
        return v;
    }
}
