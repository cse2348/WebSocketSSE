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
                // 연결/유지용 프레임은 모두 허용
                .simpTypeMatchers(
                        SimpMessageType.CONNECT,
                        SimpMessageType.HEARTBEAT,
                        SimpMessageType.DISCONNECT
                ).permitAll()

                // 애플리케이션 전송 (SEND: /app/**) → 인증 필수
                .simpDestMatchers("/app/**").authenticated()

                // 구독 (SUBSCRIBE: /topic/**, /queue/**, /user/**) → 인증 필수
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()

                // 나머지 메시지는 전부 차단
                .anyMessage().denyAll();

        return messages.build();
    }
}
