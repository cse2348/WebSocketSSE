package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component // 스프링 빈 등록
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil; // JWT 토큰 검증 및 유저 정보 추출 유틸

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message); // STOMP 헤더 추출

        // CONNECT 명령일 때 JWT 검증 수행
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) { // enum으로 안전 비교
            String token = accessor.getFirstNativeHeader("Authorization"); // Authorization 헤더 값 가져오기
            if (token == null || !token.startsWith("Bearer ")) { //  누락 시 명확히 에러발생
                throw new IllegalArgumentException("Missing Authorization Bearer token in STOMP CONNECT");
            }
            token = token.substring(7); // Bearer 제거하여 실제 토큰만 추출

            // JwtUtil로 토큰 검증 및 사용자 ID 추출
            Long userId = jwtUtil.validateAndGetUserId(token);

            // STOMP 세션에 Principal(사용자) 정보 설정 (userId를 name으로 보관)
            accessor.setUser((Principal) () -> String.valueOf(userId)); // Principal 주입 보장
        }
        return message; // 메시지 그대로 반환
    }
}
