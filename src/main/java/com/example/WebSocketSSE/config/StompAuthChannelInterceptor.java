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

import java.security.Principal;
import java.util.List;

@Slf4j // 로깅용 어노테이션
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil; // JWT 유틸

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor acc = StompHeaderAccessor.wrap(message); // STOMP 헤더 접근 도우미

        final StompCommand cmd = acc.getCommand(); // STOMP 명령 추출
        if (cmd == null) { // HEARTBEAT 등 명령 없는 프레임
            ensureUserFromSecurityContext(acc); // 사용자 복구만 하고 통과
            return message;
        }

        // CONNECT 시 토큰 인증
        if (StompCommand.CONNECT.equals(cmd)) {
            // CONNECT 명령은 반드시 Authorization 헤더가 있어야 함
            try {
                log.debug("[WS] CONNECT 시도. headers={}", toNativeHeaderMapSafe(acc));
                String token = resolveToken(acc); // 헤더에서 토큰 추출
                Authentication authentication = jwtUtil.getAuthentication(token); // 인증 객체 생성
                SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext 저장
                acc.setUser(authentication); // STOMP 세션에 사용자 연결
                log.info("[WS] CONNECT 인증 성공. user={}", authentication.getName());
            } catch (IllegalArgumentException iae) { // 토큰 없거나 잘못됨
                log.error("[WS] CONNECT 인증 실패(요청 형식): {}", iae.getMessage());
                throw new MessageDeliveryException("CONNECT 인증 실패: " + iae.getMessage());
            } catch (Exception e) { // 토큰 검증 실패
                log.error("[WS] CONNECT 인증 실패(검증): {}", e.getMessage());
                throw new MessageDeliveryException("CONNECT 인증 실패: " + e.getMessage());
            }
            return message;
        }

        // CONNECT 이후 사용자 정보가 없으면 SecurityContext에서 복구
        ensureUserFromSecurityContext(acc);
        // CONNECT 이후에는 사용자 정보가 있어야 함
        return message;
    }

    // STOMP 헤더에서 JWT 토큰 추출
    private String resolveToken(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String header = firstHeader(accessor, "Authorization");
        // 대소문자 구분 없이 헤더 키 찾기
        if (header == null) header = firstHeader(accessor, "authorization");
        // 대소문자 구분 없이 Bearer 토큰 찾기
        if (header == null) header = firstHeader(accessor, "access_token");
        // 대소문자 구분 없이 Bearer 토큰 찾기 (구식 클라이언트 호환)
        if (header == null || header.isBlank()) throw new IllegalArgumentException("Authorization 헤더를 찾을 수 없습니다.");
        // Bearer 접두사 제거
        String token = header.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) token = token.substring(7).trim(); // Bearer 제거
        if (token.isEmpty()) throw new IllegalArgumentException("토큰이 비어있습니다.");
        return token;
    }

    // 특정 헤더 키에서 첫 번째 값 반환
    private String firstHeader(StompHeaderAccessor acc, String key) {
        // 대소문자 구분 없이 헤더 키 찾기
        List<String> vals = acc.getNativeHeader(key);
        // 헤더가 없거나 비어있으면 null 반환
        if (vals == null || vals.isEmpty()) return null;
        // 첫 번째 값이 null 또는 빈 문자열이면 null 반환
        String v = vals.get(0);
        // 빈 문자열 또는 null이면 null 반환
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // CONNECT 이후 user 없으면 SecurityContext에서 복구
    private void ensureUserFromSecurityContext(StompHeaderAccessor acc) {
        // 현재 STOMP 세션에 사용자 정보가 없으면 SecurityContext에서 복구
        Principal current = acc.getUser();
        // 현재 사용자 정보가 없으면 SecurityContext에서 가져옴
        if (current == null) {
            // SecurityContextHolder에서 현재 인증 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // 인증 정보가 있고, 현재 STOMP 세션에 사용자 정보가 없으면 설정
            if (auth != null) {
                // 현재 STOMP 세션에 사용자 정보 설정
                acc.setUser(auth);
                // 디버그 로그 출력
                log.debug("[WS] 프레임에 사용자 복구: {}", auth.getName());
            }
        }
    }

    // 로그용: 헤더 맵 출력 (NullPointer 방지)
    private Object toNativeHeaderMapSafe(StompHeaderAccessor acc) {
        try {
            // Native 헤더 맵을 안전하게 반환
            return acc.toNativeHeaderMap();
        } catch (Throwable ignore) {
            // 예외 발생 시 null 대신 (unavailable) 반환
            return "(unavailable)";
        }
    }
}
