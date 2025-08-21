package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * STOMP CONNECT 시 Authorization 헤더에서 JWT를 꺼내 Authentication을 세팅하는 채널 인터셉터.
 * - CONNECT 프레임: 토큰 검증 → SecurityContext + STOMP 세션에 사용자 저장
 * - 그 외 프레임: 사용자 정보(acc.getUser) 없으면 SecurityContext에서 복원(안전장치)
 *
 * 주의: 이 인터셉터는 반드시 "inbound channel" 에 등록되어야 함.
 *   WebSocketConfig.configureClientInboundChannel(..)에서 registration.interceptors(thisInterceptor) 호출 필요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

        // 0) 방어 코드: null 커맨드(HEARTBEAT 등) 처리
        final StompCommand cmd = acc.getCommand();
        if (cmd == null) {
            // HEARTBEAT 같은 프레임은 사용자 컨텍스트만 살려주고 통과
            ensureUserFromSecurityContext(acc);
            return message;
        }

        // 1) CONNECT 단계에서만 헤더 파싱 + 인증 세팅
        if (StompCommand.CONNECT.equals(cmd)) {
            try {
                log.debug("[WS] CONNECT 시도. headers={}", toNativeHeaderMapSafe(acc));

                // (1) 헤더에서 JWT 토큰을 추출
                String token = resolveToken(acc);

                // (2) 토큰으로 Authentication 생성 (JwtUtil.getAuthentication는 반드시 유효성 검사 및 권한 세팅)
                Authentication authentication = jwtUtil.getAuthentication(token);

                // (3) SecurityContext에 저장 (이후 같은 스레드 체인/프레임에서 접근 가능)
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // (4) STOMP 세션(프레임)에 Principal 연결 (메시지 보안에서 principal 기반 인가를 위해 필수)
                acc.setUser(authentication);

                log.info("[WS] CONNECT 인증 성공. user={}", authentication.getName());

            } catch (IllegalArgumentException iae) {
                // 토큰이 없거나 비어있을 때: 클라이언트에게 명시적으로 이유를 전달
                log.error("[WS] CONNECT 인증 실패(요청 형식): {}", iae.getMessage());
                throw new MessageDeliveryException("CONNECT 인증 실패: " + iae.getMessage());
            } catch (Exception e) {
                // 파싱/검증/빌드 과정 예외
                log.error("[WS] CONNECT 인증 실패(검증): {}", e.getMessage());
                throw new MessageDeliveryException("CONNECT 인증 실패: " + e.getMessage());
            }
            return message;
        }

        // 2) CONNECT 이후 프레임(SUBSCRIBE, SEND, DISCONNECT 등)
        //    간혹 프레임에 user가 비어있는 경우가 있어, SecurityContext에서 복구해 세션에 연결해 준다.
        ensureUserFromSecurityContext(acc);

        return message;
    }

    /**
     * STOMP 헤더에서 JWT 토큰을 추출.
     * - 우선순위: Authorization → authorization → access_token
     * - "Bearer " 접두사 허용(대소문자 무시)
     */
    private String resolveToken(StompHeaderAccessor accessor) {
        String header = firstHeader(accessor, "Authorization");
        if (header == null) header = firstHeader(accessor, "authorization");
        if (header == null) header = firstHeader(accessor, "access_token");

        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Authorization 헤더를 찾을 수 없습니다.");
        }

        String token = header.trim();
        // "Bearer " 접두사가 있으면 제거(대소문자 무시)
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        if (token.isEmpty()) {
            throw new IllegalArgumentException("토큰이 비어있습니다.");
        }
        return token;
    }

    /**
     * 특정 키의 첫 번째 native 헤더 값을 안전하게 반환.
     */
    private String firstHeader(StompHeaderAccessor acc, String key) {
        List<String> vals = acc.getNativeHeader(key);
        if (vals == null || vals.isEmpty()) return null;
        String v = vals.get(0);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /**
     * CONNECT 이후 프레임에서 user가 비어있을 때 SecurityContext로부터 복구.
     * - 일부 환경/클라이언트 조합에서 프레임 단위로 Principal이 비어 전달되는 경우가 있어 안전장치로 둔다.
     */
    private void ensureUserFromSecurityContext(StompHeaderAccessor acc) {
        Principal current = acc.getUser();
        if (current == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                acc.setUser(auth);
                log.debug("[WS] 프레임에 사용자 복구: {}", auth.getName());
            }
        }
    }

    /**
     * 로그용: 헤더 맵 안전 출력(NullPointer 방지)
     */
    private Object toNativeHeaderMapSafe(StompHeaderAccessor acc) {
        try {
            return acc.toNativeHeaderMap();
        } catch (Throwable ignore) {
            return "(unavailable)";
        }
    }
}
