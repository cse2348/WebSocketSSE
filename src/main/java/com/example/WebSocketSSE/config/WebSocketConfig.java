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
        registry.enableSimpleBroker("/topic");      // 구독 경로
        registry.setApplicationDestinationPrefixes("/app"); // 발행 경로 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*"); // 운영에선 프론트 도메인으로 제한 권장
        // SockJS 필요 없으면 생략
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(stompAuthChannelInterceptor) // CONNECT에서 인증 셋업(예외 던지지 않도록!)
                .taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(16)
                .queueCapacity(1000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(64 * 1024);
        reg.setSendBufferSizeLimit(512 * 1024);
        reg.setSendTimeLimit(20_000);
    }
}
