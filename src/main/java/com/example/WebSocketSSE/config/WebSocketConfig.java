package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(
                        "https://winnerteam.store",
                        "https://backendteamb.site",
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "*" // 개발 편의. 운영에서는 특정 도메인만 허용
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                inboundLoggingInterceptor(),       // 1) 로깅
                stompAuthChannelInterceptor,       // 2) JWT 인증
                new SecurityContextChannelInterceptor() // 3) SecurityContext 전파
        );
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration reg) {
        reg.setMessageSizeLimit(64 * 1024);
        reg.setSendBufferSizeLimit(512 * 1024);
        reg.setSendTimeLimit(20_000);
    }

    // 인바운드 로깅 인터셉터
    @Bean
    public ChannelInterceptor inboundLoggingInterceptor() {
        return new ChannelInterceptor() {
            private final Logger log = LoggerFactory.getLogger("WS-INBOUND");

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var acc = StompHeaderAccessor.wrap(message);
                log.debug("preSend cmd={} headers={}", acc.getCommand(), acc.toNativeHeaderMap());
                return message;
            }

            @Override
            public void afterSendCompletion(
                    Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
                if (ex != null) {
                    var acc = StompHeaderAccessor.wrap(message);
                    log.error("afterSendCompletion cmd={} ERROR: {}", acc.getCommand(), ex.toString(), ex);
                }
            }
        };
    }

    // STOMP ERROR 프레임 커스터마이징 (클라에 상세 사유 전달)
    @Bean
    public StompSubProtocolErrorHandler stompSubProtocolErrorHandler() {
        return new StompSubProtocolErrorHandler() {
            @Override
            public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
                String msg = "DEBUG: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());

                StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
                accessor.setMessage(msg);
                accessor.setLeaveMutable(true);

                byte[] payload = msg.getBytes(StandardCharsets.UTF_8);

                return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
            }
        };
    }
}
