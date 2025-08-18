package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;

@Configuration
@EnableWebSocketSecurity // WebSocket 보안 활성화
// WebSocket 메시지에 대한 권한 관리를 설정하는 클래스
public class WebSocketSecurityConfig {

    // 권장: Builder를 파라미터로 주입받아 사용
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        messages
                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll()               // JWT 검증은 Interceptor에서 수행
                .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll()
                .simpDestMatchers("/app/**").authenticated()                         // SEND(발행) 권한
                .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated() // 구독 권한
                .anyMessage().denyAll();                                    // 보수적으로 차단

        return messages.build();
    }
}
