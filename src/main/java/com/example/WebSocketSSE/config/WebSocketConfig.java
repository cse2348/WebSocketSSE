package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    // STOMP CONNECT 시 JWT 검증 인터셉터 주입

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // 클라이언트가 연결할 STOMP 엔드포인트 경로
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS 지원 (브라우저 환경 호환성 ↑, 필요 없으면 제거 가능)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 prefix: 클라이언트가 subscribe 할 때 /topic/** 로 구독
        registry.enableSimpleBroker("/topic", "/queue");

        //발행 prefix: 클라이언트가 메시지 보낼 때 /app/** 로 전송 → @MessageMapping 핸들러로 매핑
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 시 토큰 검증 인터셉터 등록
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
