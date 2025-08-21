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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
        StompCommand cmd = acc.getCommand();
        String sessionId = acc.getSessionId();

        try {
            if (StompCommand.CONNECT.equals(cmd)) {
                // 1) CONNECT: 헤더에서 토큰 추출 → 인증 생성
                String token = resolveTokenFromHeaders(acc);
                Authentication auth = jwtUtil.getAuthentication(token);

                // 세션 Principal 지정
                acc.setUser(auth);

                // SecurityContext에도 심기
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(auth);
                SecurityContextHolder.setContext(ctx);

                // (안전망) 세션 속성에도 보관
                Map<String, Object> attrs = acc.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("SPRING.PRINCIPAL", auth);
                }

                log.info("[WS] CONNECT 인증 성공: session={}, user={}", sessionId, auth.getName());

            } else if (StompCommand.SEND.equals(cmd) || StompCommand.SUBSCRIBE.equals(cmd)) {
                // 2) SEND/SUBSCRIBE: 세션에 Principal 유지 확인
                if (acc.getUser() == null) {
                    // SecurityContext에 있으면 보강
                    Authentication current = SecurityContextHolder.getContext().getAuthentication();
                    if (current != null) {
                        acc.setUser(current);
                    } else {
                        // 명시적 에러 프레임 전송
                        throw new MessageDeliveryException("Unauthenticated " + cmd + " frame");
                    }
                }
                log.debug("[WS] {} 인가 확인: session={}, user={}", cmd, sessionId, acc.getUser().getName());
            }
            // DISCONNECT 등은 통과
            return message;

        } catch (Exception e) {
            // 예외를 던져야 STOMP ERROR 프레임이 내려감
            log.error("[WS] {} 처리 실패: session={}, reason={}", cmd, sessionId, e.toString(), e);
            throw new MessageDeliveryException("STOMP " + cmd + " rejected: " + e.getMessage());
        }
    }

    // Authorization/authorization/AUTHORIZATION/access_token 지원 + Bearer 유무 모두 허용
    private String resolveTokenFromHeaders(StompHeaderAccessor acc) {
        String h = first(acc, "Authorization");
        if (h == null) h = first(acc, "authorization");
        if (h == null) h = first(acc, "AUTHORIZATION");
        if (h == null) h = first(acc, "access_token");
        if (h == null || h.isBlank()) {
            throw new IllegalArgumentException("Authorization header missing");
        }
        String v = h.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) v = v.substring(7).trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Empty token");
        return v;
    }

    private String first(StompHeaderAccessor acc, String key) {
        String v = acc.getFirstNativeHeader(key);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
