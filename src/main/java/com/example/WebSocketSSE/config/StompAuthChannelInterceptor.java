package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.jwt.JwtUtil;
import com.example.WebSocketSSE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        //`
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP CONNECT 요청 시 Authorization 헤더에서 JWT 토큰 추출 및 검증
            try {
                String token = accessor.getFirstNativeHeader("Authorization");
                System.out.println("[STOMP] Authorization 헤더: " + token);

                if (token == null || !token.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Missing or invalid Authorization header in STOMP CONNECT");
                }

                token = token.substring(7); // "Bearer " 제거
                System.out.println("[STOMP] 추출된 토큰: " + token);

                // 1) JWT 로 username 가져오기
                Authentication authentication = jwtUtil.getAuthentication(token);

                if (authentication == null) {
                    throw new IllegalArgumentException("Invalid JWT token");
                }

                // 2) username → userId 변환
                Long userId = userRepository.findByUsername(authentication.getName())
                        .map(User::getId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + authentication.getName()));

                // 3) Principal 에 userId 저장 (String)
                Authentication simpAuth = new UsernamePasswordAuthenticationToken(
                        userId.toString(), // principal.getName() → userId 로 들어감
                        null,
                        authentication.getAuthorities()
                );

                accessor.setUser(simpAuth);
                System.out.println("[STOMP] 인증 성공 - 사용자 ID: " + userId);

            } catch (Exception e) {
                System.out.println("[STOMP] 인증 처리 중 예외 발생: " + e.getMessage());
                throw e;
            }
        }
        return message;
    }
}
