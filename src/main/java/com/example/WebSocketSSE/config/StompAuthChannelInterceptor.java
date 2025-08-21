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
import org.springframework.security.core.context.SecurityContext;
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
                // 다양한 키 시도 (대소문자/대체키)
                String rawHeader = firstHeader(acc, "Authorization");
                if (rawHeader == null) rawHeader = firstHeader(acc, "authorization");
                if (rawHeader == null) rawHeader = firstHeader(acc, "AUTHORIZATION");
                if (rawHeader == null) rawHeader = firstHeader(acc, "access_token");

                log.debug("[WS] CONNECT headers={}, pickedAuthHeader={}", acc.toNativeHeaderMap(), rawHeader);

                if (rawHeader != null && !rawHeader.isBlank()) {
                    String token = normalizeToToken(rawHeader);       // Bearer 제거
                    Authentication auth = jwtUtil.getAuthentication(token); // 구현: UsernamePasswordAuthenticationToken

                    // STOMP 세션 Principal 지정
                    acc.setUser(auth);

                    // SecurityContext에도 등록
                    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(auth);
                    SecurityContextHolder.setContext(ctx);

                    log.info("[WS] CONNECT 인증 성공. principal={}, name={}", acc.getUser(), auth.getName());
                } else {
                    log.info("[WS] CONNECT 익명 시도(Authorization 없음)");
                }
            } catch (Exception e) {
                log.error("[WS] CONNECT 인증 실패", e);
                // 정책상 끊고 싶으면 예외 던지기
                // throw e;
            }
        }
        return message;
    }

    private String firstHeader(StompHeaderAccessor acc, String key) {
        String v = acc.getFirstNativeHeader(key);
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

    private String normalizeToToken(String headerValue) {
        String v = headerValue.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return v.substring(7).trim();
        }
        return v;
    }
}
