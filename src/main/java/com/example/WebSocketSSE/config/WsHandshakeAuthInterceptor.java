package com.example.WebSocketSSE.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WsHandshakeAuthInterceptor implements HandshakeInterceptor {
    private final com.example.WebSocketSSE.util.JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
        String token = null;
        var query = req.getURI().getQuery();
        if (query != null) {
            token = Arrays.stream(query.split("&"))
                    .filter(s -> s.startsWith("token="))
                    .map(s -> s.substring(6))
                    .findFirst().orElse(null);
        }
        Long userId = jwtUtil.validateAndGetUserId(token);
        attrs.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler wsHandler, Exception ex) { }
}
