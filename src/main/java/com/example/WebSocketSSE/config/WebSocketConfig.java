package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // HTML 클라와 일치: 구독(/topic, /queue, /user), 발행(/app)
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 브라우저 Origin 허용
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(
                        "https://winnerteam.store",
                        "https://backendteamb.site",
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "*" // 개발 편의. 운영에서 제거 권장
                );
        // SockJS 미사용 (네 HTML은 native WebSocket 사용)
        // .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration
                .interceptors(stompAuthChannelInterceptor, new SecurityContextChannelInterceptor())
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
