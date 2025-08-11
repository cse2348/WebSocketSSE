package com.example.WebSocketSSE.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = StompHeaderAccessor.wrap(message);
        var attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.containsKey("userId")) {
            Long userId = (Long) attrs.get("userId");
            accessor.setUser((Principal) () -> String.valueOf(userId));
        }
        return message;
    }
}
