package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class WebSocketAuthConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public ChannelInterceptor jwtChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                // CONNECT 시 토큰 검증 → Authentication 생성 → SecurityContext + 세션(Principal) 주입
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);

                        if (jwtUtil.validateToken(token)) {
                            Long userId = jwtUtil.validateAndGetUserId(token);

                            UserDetails principal = User.withUsername(String.valueOf(userId))
                                    .password("") // JWT 인증이라 불필요
                                    .authorities(Collections.emptyList())
                                    .build();

                            Authentication auth =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                            // SecurityContext 에도 저장 (컨트롤러 @MessageMapping Principal 보장)
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            // STOMP 세션(Principal)에도 저장
                            accessor.setUser(auth);
                        }
                    }
                }
                // SEND/SUBSCRIBE/UNSUBSCRIBE 시 보안 컨텍스트 비어 있으면 세션의 Principal로 복원
                else if (StompCommand.SEND.equals(accessor.getCommand())
                        || StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        || StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                    if (SecurityContextHolder.getContext().getAuthentication() == null
                            && accessor.getUser() instanceof Authentication auth) {
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
                // (선택) DISCONNECT 시 컨텍스트 정리
                else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    SecurityContextHolder.clearContext();
                }

                return message;
            }
        };
    }
}
