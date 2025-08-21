package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                // WebSocket 연결(CONNECT) 자체는 누구나 시도할 수 있도록 허용
                // (실제 인증은 StompAuthChannelInterceptor에서 JWT로 처리)
                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()

                // 그 외 모든 메시지(SEND, SUBSCRIBE 등)는 인증된 사용자만 가능
                .anyMessage().authenticated();

        return messages.build();
    }
}