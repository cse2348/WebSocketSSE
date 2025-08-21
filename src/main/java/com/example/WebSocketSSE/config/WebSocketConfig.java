package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket과 STOMP 프로토콜을 사용하기 위한 설정 클래스입니다.
 * @EnableWebSocketMessageBroker: STOMP 메시징을 활성화합니다.
 * WebSocketMessageBrokerConfigurer: WebSocket 관련 설정을 커스터마이징할 수 있는 메서드들을 제공합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 우리가 직접 만든 JWT 인증용 인터셉터입니다.
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    /**
     * 메시지 브로커 관련 설정을 담당합니다.
     * 메시지 브로커는 일종의 '우체국' 역할을 하며, 클라이언트 간 메시지를 중계합니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. Simple Broker 설정:
        //    - "/topic", "/queue", "/user"로 시작하는 주소를 구독하는 클라이언트에게 메시지를 전달합니다.
        //    - Spring이 기본으로 제공하는 인-메모리 방식의 간단한 메시지 브로커입니다.
        registry.enableSimpleBroker("/topic", "/queue", "/user")
                // 하트비트(ping/pong) 설정: 서버와 클라이언트 간의 연결 상태를 주기적으로 확인합니다.
                // 값 [10000, 10000]은 "서버는 최소 10초마다 ping을 보내고, 클라이언트도 최소 10초마다 ping을 보내야 한다"는 의미입니다.
                .setHeartbeatValue(new long[]{10_000L, 10_000L});
        // ★★★ 충돌을 일으켰던 .setTaskScheduler(...) 부분을 제거했습니다. Spring Boot의 기본 스케줄러를 사용합니다.

        // 2. Application Destination Prefix 설정:
        //    - 클라이언트가 서버로 메시지를 보낼 때 사용하는 주소의 접두사입니다.
        //    - 예를 들어, 클라이언트가 "/app/chat"으로 메시지를 보내면, @MessageMapping("/chat") 어노테이션이 붙은 메서드가 처리합니다.
        registry.setApplicationDestinationPrefixes("/app");

        // 3. User Destination Prefix 설정:
        //    - 특정 사용자에게 1:1 메시지를 보낼 때 사용하는 주소의 접두사입니다.
        //    - 예를 들어, "/user/queue/private"와 같은 주소로 메시지를 보낼 수 있습니다.
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 프로토콜을 사용하기 위한 엔드포인트를 등록합니다.
     * 엔드포인트는 클라이언트가 서버에 WebSocket 핸드셰이크를 하기 위해 접속하는 주소입니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // "/ws/chat" 주소로 WebSocket 연결을 위한 엔드포인트를 등록합니다.
        registry.addEndpoint("/ws/chat")
                // CORS(Cross-Origin Resource Sharing) 설정: 다른 도메인에서의 접속을 허용합니다.
                .setAllowedOriginPatterns(
                        "https://winnerteam.store",
                        "https://backendteamb.site",
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "*" // 개발 편의상 모든 Origin을 허용. 운영 환경에서는 보안을 위해 특정 도메인만 명시하는 것이 좋습니다.
                );
        // SockJS는 WebSocket을 지원하지 않는 낡은 브라우저를 위한 대체 옵션입니다.
        // 최근 브라우저들은 대부분 WebSocket을 지원하므로, 사용하지 않도록 주석 처리했습니다.
        // .withSockJS();
    }

    /**
     * 클라이언트로부터 들어오는 메시지(inbound)를 처리하는 채널을 설정합니다.
     * 인터셉터를 등록하여 메시지가 처리되기 전에 특정 로직을 수행할 수 있습니다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                // 1. stompAuthChannelInterceptor:
                //    - 우리가 직접 만든 JWT 인증 인터셉터입니다.
                //    - 클라이언트가 CONNECT할 때 헤더의 JWT 토큰을 검증하는 역할을 합니다.
                stompAuthChannelInterceptor
                // ★★★ new SecurityContextChannelInterceptor()를 제거했습니다.
                // `spring-security-messaging` 의존성이 있으면 Spring Security가 자동으로 이 인터셉터를 등록해주므로,
                // 수동으로 추가하면 중복 등록될 수 있습니다.
        );
    }

    /**
     * WebSocket 전송 관련 설정을 담당합니다. (메시지 크기 제한 등)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(64 * 1024);      // 메시지 최대 크기 (64KB)
        reg.setSendBufferSizeLimit(512 * 1024);  // 전송 버퍼 크기 (512KB)
        reg.setSendTimeLimit(20_000);          // 전송 시간 제한 (20초)
    }

}