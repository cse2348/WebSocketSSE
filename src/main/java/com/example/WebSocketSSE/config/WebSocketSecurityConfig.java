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
                // 연결 유지에 필요한 STOMP 프레임은 오픈
                .simpTypeMatchers(
                        SimpMessageType.CONNECT,
                        SimpMessageType.HEARTBEAT,
                        SimpMessageType.DISCONNECT,
                        SimpMessageType.UNSUBSCRIBE
                ).permitAll()

                // 앱으로 들어오는 SEND는 인증 필요
                .simpDestMatchers("/app/**").authenticated()

                // 토픽/큐 구독은 인증 필요
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()

                // 나머지는 최소 authenticated (denyAll은 디버깅/운영에 방해)
                .anyMessage().authenticated();

        return messages.build();
    }
}
