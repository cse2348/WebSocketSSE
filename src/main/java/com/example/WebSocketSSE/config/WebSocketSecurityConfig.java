package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        // Builder를 직접 생성 (의존성 주입 문제 회피)
        MessageMatcherDelegatingAuthorizationManager.Builder messages =
                MessageMatcherDelegatingAuthorizationManager.builder();

        messages
                // /app/** 목적지로 오는 모든 메시지(SEND/SUBSCRIBE 등)는 인증 필요
                .simpDestMatchers("/app/**").authenticated()
                // 그 외는 허용 (CONNECT 등)
                .anyMessage().permitAll();

        return messages.build();
    }
}
