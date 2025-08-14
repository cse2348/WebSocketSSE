package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 프레임일 때만 JWT 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

            // Authorization 헤더가 없으면 token 헤더로 시도
            if (!StringUtils.hasText(auth)) {
                String tokenOnly = accessor.getFirstNativeHeader("token");
                if (StringUtils.hasText(tokenOnly)) {
                    auth = tokenOnly.startsWith("Bearer ") ? tokenOnly : "Bearer " + tokenOnly;
                }
            }

            // JWT 검증
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                var jws = jwtUtil.parse(token);
                String username = jws.getBody().getSubject();
                String role = (String) jws.getBody().get("role");

                var authentication = new AbstractAuthenticationToken(
                        List.of(new SimpleGrantedAuthority(role))) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return username; }
                };
                authentication.setAuthenticated(true);

                // Principal 설정 → @MessageMapping 메서드에서 Principal로 접근 가능
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return (String) authentication.getPrincipal();
                    }
                });

            } else {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
        }
        return message;
    }
}
