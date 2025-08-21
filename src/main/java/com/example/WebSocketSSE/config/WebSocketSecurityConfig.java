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
                        SimpMessageType.DISCONNECT,
                        SimpMessageType.UNSUBSCRIBE
                ).permitAll()

                // /app/** → @MessageMapping → 인증 필요
                .simpDestMatchers("/app/**").authenticated()

                // topic/queue/user 구독은 인증 필요
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()

                // 나머지는 차단
                .anyMessage().denyAll();

        return messages.build();
    }
}
