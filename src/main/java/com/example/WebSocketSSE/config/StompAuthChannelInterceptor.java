package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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
        if ("CONNECT".equals(accessor.getCommand().name())) { // STOMP CONNECT 요청 확인
            String token = accessor.getFirstNativeHeader("Authorization"); // Authorization 헤더 값 가져오기
            if (token != null && token.startsWith("Bearer ")) { // Bearer 토큰 형식인지 확인
                token = token.substring(7); // "Bearer " 제거하여 실제 토큰만 추출

                // JwtUtil로 토큰 검증 및 사용자 ID 추출
                Long userId = jwtUtil.validateAndGetUserId(token);

                // STOMP 세션에 Principal(사용자) 정보 설정
                accessor.setUser((Principal) () -> String.valueOf(userId));
            }
        }
        return message; // 메시지 그대로 반환
    }
}