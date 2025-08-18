package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.messaging.simp.SimpMessageType;

@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        // Builder를 직접 생성 (의존성 주입 문제 회피)
        MessageMatcherDelegatingAuthorizationManager.Builder messages =
                MessageMatcherDelegatingAuthorizationManager.builder();

        messages
                // 애플리케이션 목적지로의 전송(/app/**)은 인증 필요
                .simpDestMatchers("/app/**").authenticated()
                // 브로커 구독 경로: 채팅방/개인 큐 구독도 인증 요구
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()
                // CONNECT/DISCONNECT 등은 모두 허용
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT).permitAll()
                // 그 외 나머지는 일단 허용 (필요시 tightened)
                .anyMessage().permitAll();

        return messages.build();
    }
}
