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
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // (선택) 명시하면 더 명확
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(
                        "https://winnerteam.store",
                        "https://backendteamb.site",
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "*" // 개발 편의. 운영에선 특정 도메인만
                );
        // .withSockJS(); // native WebSocket 사용
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        //  반드시 함께 등록 (SecurityContext 전파)
        registration.interceptors(
                stompAuthChannelInterceptor,
                new SecurityContextChannelInterceptor()
        );
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(64 * 1024);
        reg.setSendBufferSizeLimit(512 * 1024);
        reg.setSendTimeLimit(20_000);
    }
}
