package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ========================
        // 메시지 브로커 설정
        // ========================
        registry.enableSimpleBroker("/sub");            // 구독 경로 prefix
        registry.setApplicationDestinationPrefixes("/pub"); // 발행 경로 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ========================
        // STOMP 연결 엔드포인트 설정
        // ========================
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*"); // 운영 환경에서는 프론트 도메인으로 제한 권장
        // 필요시 SockJS fallback 지원
        // .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // ========================
        // Inbound 채널 인터셉터 설정
        // ========================
        registration
                .interceptors(stompAuthChannelInterceptor)
                .taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(16)
                .queueCapacity(1000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        // ========================
        // 전송 설정 (메시지 크기 등)
        // ========================
        reg.setMessageSizeLimit(64 * 1024);
        reg.setSendBufferSizeLimit(512 * 1024);
        reg.setSendTimeLimit(20_000);
    }
}
