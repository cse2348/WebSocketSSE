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

    private final JwtUtil jwtUtil;

    @Bean
    public ChannelInterceptor jwtChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);

                        if (jwtUtil.validateToken(token)) {
                            Long userId = jwtUtil.validateAndGetUserId(token);

                            UserDetails principal = User.withUsername(String.valueOf(userId))
                                    .password("")
                                    .authorities(Collections.emptyList())
                                    .build();

                            Authentication auth =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                            accessor.setUser(auth); // STOMP 세션에 사용자 등록
                        }
                    }
                }
                return message;
            }
        };
    }
}
