package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        MessageMatcherDelegatingAuthorizationManager.Builder messages =
                MessageMatcherDelegatingAuthorizationManager.builder();

        messages
                // 클라이언트가 /app/** 로 전송하는 메시지는 인증 필요
                .simpDestMatchers("/app/**").authenticated()
                // 구독 경로도 인증 필요
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()
                // CONNECT, DISCONNECT 는 열어둠
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT).permitAll()
                // 나머지 전부 허용 (필요하면 tightened)
                .anyMessage().permitAll();

        return messages.build();
    }
}
