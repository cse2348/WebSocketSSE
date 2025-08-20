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
                if (rawHeader == null) rawHeader = firstHeader(acc, "access_token"); // 쿼리 파라미터처럼 보내는 경우 대응

                log.debug("[WS] CONNECT headers={}, pickedAuthHeader={}",
                        acc.toNativeHeaderMap(), rawHeader);

                if (rawHeader != null && !rawHeader.isBlank()) {
                    // 2) "Bearer " 접두사 제거 (있든 없든 모두 처리)
                    String token = normalizeToToken(rawHeader);

                    // 3) JWT 토큰에서 인증 정보 추출 (principal=UserPrincipal)
                    Authentication authentication = jwtUtil.getAuthentication(token);

                    // 4) 인증 정보가 유효한 경우 STOMP 세션 Principal에 설정
                    acc.setUser(authentication);
                    log.info("[WS] CONNECT 인증 성공. principal={}, name={}",
                            acc.getUser(), authentication.getName());
                } else {
                    // (선택) Authorization 헤더 자체가 없으면 익명으로 연결
                    log.info("[WS] CONNECT 익명 연결 시도(Authorization 헤더 없음).");
                    // 정책상 차단하려면 여기서 throw
                    // throw new IllegalArgumentException("인증 실패: Authorization 헤더 누락");
                }
            } catch (Exception e) {
                // 로그를 더 자세히 찍기 (예외 메시지 + 예외 클래스명 + 전체 스택)
                log.warn("[WS] CONNECT 인증 실패: {} ({})", e.getMessage(), e.getClass().getName(), e);
                // 연결 거부: 예외 던져야 클라가 ERROR 프레임 받음
                throw new IllegalArgumentException("인증 실패: " + e.getMessage(), e);
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
