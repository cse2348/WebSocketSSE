package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity // Spring Security의 STOMP 메시지 보안 활성화 (HTTP 보안과 별개)
public class WebSocketSecurityConfig {

    // STOMP 메시지에 대한 인가 규칙 정의(누가 어떤 프레임/목적지에 접근 가능한지)
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages // 규칙 빌더(스프링이 주입)
    ) {
        messages
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT).permitAll() // 최초 CONNECT/HEARTBEAT는 모두 허용(핸드셰이크/연결 유지)
                // .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll() // 필요 시 DISCONNECT도 무조건 허용 가능
                .simpDestMatchers("/app/**").authenticated() // 클라이언트→서버로 보내는 목적지(Controller @MessageMapping) 인증 필요
                .simpSubscribeDestMatchers("/topic/**", "/user/queue/**").authenticated() // 브로커가 푸시하는 구독 목적지브로커가 푸시 인증 필요->개인 큐 포함
                .anyMessage().denyAll(); // 그 밖의 모든 메시지는 거부

        return messages.build(); // AuthorizationManager 빌드하여 빈으로 등록
    }
}
