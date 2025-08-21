package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor // 생성자 주입 자동 생성
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor; // JWT 인증용 STOMP 인터셉터

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user"); // 구독 경로(prefix) → 서버가 클라이언트에게 메시지 전달
        registry.setApplicationDestinationPrefixes("/app"); // 발행 경로(prefix) → 클라이언트가 서버로 메시지 전송할 때 사용
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // WebSocket 연결 엔드포인트
                .setAllowedOriginPatterns("*"); // CORS 허용(모든 도메인 접근 허용)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor); // 클라이언트 → 서버 방향(inbound) 인터셉터 등록 (JWT 인증 체크)
    }
}
