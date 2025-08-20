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

        // CONNECT 명령에 대해서만 인증 체크
        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                // 1) 다양한 키에서 토큰 헤더를 시도 (대소문자/대체키 포함)
                String rawHeader = firstHeader(acc, "Authorization");
                if (rawHeader == null) rawHeader = firstHeader(acc, "authorization");
                if (rawHeader == null) rawHeader = firstHeader(acc, "AUTHORIZATION");
                if (rawHeader == null) rawHeader = firstHeader(acc, "access_token");

                log.debug("[WS] CONNECT headers={}, pickedAuthHeader={}",
                        acc.toNativeHeaderMap(), rawHeader);

                if (rawHeader != null && !rawHeader.isBlank()) {
                    // 2) "Bearer " 접두사 제거 (있든 없든 모두 처리)
                    String token = normalizeToToken(rawHeader);

                    // 3) JWT 토큰에서 인증 정보 추출 (principal=UserPrincipal)
                    Authentication authentication = jwtUtil.getAuthentication(token);

                    // 4) 인증 정보가 유효한 경우 STOMP 세션 Principal에 설정
                    acc.setUser(authentication);

                    // 중앙 보안 컨텍스트(SecurityContextHolder)에도 등록
                    // 메시징 보안 체인/표현식에서 인증 정보를 인식하도록 보장
                    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(authentication);
                    SecurityContextHolder.setContext(ctx);

                    log.info("[WS] CONNECT 인증 성공. principal={}, name={}",
                            acc.getUser(), authentication.getName());
                } else {
                    log.info("[WS] CONNECT 익명 연결 시도(Authorization 헤더 없음).");
                }
            } catch (Exception e) {
                // 예외 발생 시: 인증 실패로 간주하고 연결 거부
                log.error("[WS] CONNECT 인증 과정에서 심각한 예외 발생! 원인을 확인해야 합니다.", e);
            }
        }
        return message;
    }

    // 헤더 값 하나 꺼내기 (빈 문자열은 무시)
    private String firstHeader(StompHeaderAccessor acc, String key) {
        String v = acc.getFirstNativeHeader(key);
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

    // Bearer xxx 형태면 접두사 제거, 아니면 그대로 반환
    private String normalizeToToken(String headerValue) {
        String v = headerValue.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return v.substring(7).trim();
        }
        return v;
    }
}
