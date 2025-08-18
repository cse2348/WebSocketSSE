package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Component // 스프링 빈 등록
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil; // JWT 토큰 검증 및 유저 정보 추출 유틸

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message); // STOMP 헤더 추출

        // CONNECT 명령일 때 JWT 검증 수행
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization"); // Authorization 헤더 값 가져오기
            System.out.println("[STOMP] Authorization 헤더: " + token);

            if (token == null || !token.startsWith("Bearer ")) {
                System.out.println("[STOMP] Authorization 헤더 없음 또는 형식 불일치");
                throw new IllegalArgumentException("Missing Authorization Bearer token in STOMP CONNECT");
            }

            token = token.substring(7); // Bearer 제거
            System.out.println("[STOMP] 추출된 토큰: " + token);

            // JwtUtil로 Authentication 객체 생성
            Authentication authentication = jwtUtil.getAuthentication(token);

            if (authentication != null) {
                System.out.println("[STOMP] 인증 성공 - 사용자: " + authentication.getName());
            } else {
                System.out.println("[STOMP] 인증 실패 - Authentication 객체 null");
            }

            // STOMP 세션에 Authentication 등록
            accessor.setUser(authentication);
        }

        return message; // 메시지 그대로 반환
    }
}
