package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // HTML 클라와 일치: 구독(/topic, /queue, /user), 발행(/app)
        registry.enableSimpleBroker("/topic", "/queue", "/user")
                // 하트비트 설정 (클라의 heart-beat:10000,10000 과 맞춤)
                .setHeartbeatValue(new long[]{10_000L, 10_000L})
                .setTaskScheduler(messageBrokerTaskScheduler()); // 하트비트 스케줄러

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
                        "*" // 개발 편의. 운영에서는 제거 권장
                );
        // SockJS 미사용 (네 HTML은 native WebSocket 사용)
        // .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // executor 튜닝 제거: 기본 executor 사용 (커스텀 풀/큐로 인한 초기 CONNECT 실패 방지)
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

    // SimpleBroker 하트비트용 스케줄러
    @Bean
    public TaskScheduler messageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix("ws-heartbeat-");
        ts.initialize();
        return ts;
    }
}
