package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

// 스프링 설정 클래스 지정
@Configuration
// STOMP 기반 웹소켓 메시징 활성화
@EnableWebSocketMessageBroker
// 웹소켓 메시지 브로커 설정 인터페이스 구현
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor; // STOMP CONNECT 시 JWT 검증 인터셉터 주입

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat") // 클라이언트가 연결할 STOMP 엔드포인트 경로 설정
                .setAllowedOriginPatterns("*"); // 모든 도메인에서의 접근 허용 (CORS 설정)

        registry.addEndpoint("/ws/chat") // 동일 경로를 SockJS로도 노출
                .setAllowedOriginPatterns("*")
                .withSockJS(); // WebSocket 미지원 브라우저를 위해 SockJS 사용
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 구독자에게 메시지를 전달하는 경로(prefix)
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트가 메시지를 보낼 때 사용하는 경로(prefix)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor); // STOMP CONNECT 시 JWT 검증 인터셉터 등록
    }
}
