package com.example.WebSocketSSE.config; // 본인 프로젝트 패키지에 맞게 수정

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        // STOMP의 모든 메시지는 인증된 사용자만 허용합니다.
        // (CONNECT 인증은 저희가 만든 StompAuthChannelInterceptor에서 이미 처리했습니다.)
        messages.anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}