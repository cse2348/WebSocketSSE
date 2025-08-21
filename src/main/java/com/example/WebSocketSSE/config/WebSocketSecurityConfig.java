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
                // 연결/유지용 프레임은 오픈
                .simpTypeMatchers(
                        SimpMessageType.CONNECT,
                        SimpMessageType.HEARTBEAT,
                        SimpMessageType.DISCONNECT,
                        SimpMessageType.UNSUBSCRIBE
                ).permitAll()

                // @MessageMapping("/**") -> /app/** 로 들어오는 SEND는 인증 필수
                .simpDestMatchers("/app/**").authenticated()

                // 구독은 인증 필수 (원하면 .permitAll()로 완화 가능)
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()

                // 목적지 없는 기타 프레임/예외 케이스는 전부 차단
                .anyMessage().denyAll();

        return messages.build();
    }
}
