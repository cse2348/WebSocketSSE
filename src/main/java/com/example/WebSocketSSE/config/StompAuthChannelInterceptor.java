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

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                // 디버깅용: 들어온 헤더 한번 남겨두기
                log.debug("[WS] CONNECT headers={}", acc.toNativeHeaderMap());

                String token = resolveToken(acc);                // 여러 키/포맷 허용
                Authentication authentication = jwtUtil.getAuthentication(token);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                acc.setUser(authentication);

                log.info("[WS] CONNECT 인증 성공. user={}", authentication.getName());

            } catch (Exception e) {
                log.error("[WS] CONNECT 인증 실패: {}", e.toString(), e);
                // 원인이 담긴 ERROR 프레임을 내려보내기 위해 MessageDeliveryException으로 던짐
                throw new MessageDeliveryException("CONNECT rejected: " + e.getMessage());
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String h = first(accessor, "Authorization");
        if (h == null) h = first(accessor, "authorization");
        if (h == null) h = first(accessor, "AUTHORIZATION");
        if (h == null) h = first(accessor, "access_token");
        if (h == null || h.isBlank()) throw new IllegalArgumentException("Authorization header missing");

        String v = h.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) v = v.substring(7).trim(); // Bearer 접두사 있으면 제거
        if (v.isEmpty()) throw new IllegalArgumentException("Empty token");
        return v;
    }

    private String first(StompHeaderAccessor acc, String key) {
        String v = acc.getFirstNativeHeader(key);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
