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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class WebSocketAuthConfig {

    private final JwtUtil jwtUtil; // 기존 JwtUtil 사용

    @Bean
    public ChannelInterceptor jwtChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // STOMP CONNECT frame 에서 Authorization 헤더 추출
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);

                        if (jwtUtil.validateToken(token)) {
                            String username = jwtUtil.getUsernameFromToken(token);

                            // 간단한 Authentication 객체 생성
                            UserDetails principal = User.withUsername(username)
                                    .password("") // 비번은 불필요
                                    .authorities(Collections.emptyList())
                                    .build();

                            Authentication auth =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                            accessor.setUser(auth); // 세션 사용자 지정
                        }
                    }
                }
                return message;
            }
        };
    }
}
