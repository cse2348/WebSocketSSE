package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        messages
                // /app/** 목적지로 오는 모든 메시지(SEND, SUBSCRIBE 등)는 인증된 사용자만 허용
                .simpDestMatchers("/app/**").authenticated()
                // 그 외 다른 모든 메시지 유형은 일단 허용 (CONNECT 등)
                .anyMessage().permitAll();

        return messages.build();
    }
}
