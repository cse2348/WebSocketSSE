package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            try {
                // 1) Authorization 헤더(대/소문자 모두) 조회
                String auth = acc.getFirstNativeHeader("Authorization");
                if (auth == null) auth = acc.getFirstNativeHeader("authorization");

                if (auth != null && !auth.isBlank()) {
                    // 2) JwtUtil이 Bearer 제거 처리하도록 맡김(내부에서 strip 처리)
                    Long userId = jwtUtil.validateAndGetUserId(auth);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId), null, Collections.emptyList());
                    acc.setUser(authentication);
                    log.info("[WS] CONNECT auth OK userId={}", userId);
                } else {
                    // Authorization 없으면 익명으로 통과 (SUBSCRIBE/SEND에서 policy로 걸러짐)
                    log.info("[WS] CONNECT without Authorization (anonymous)");
                }
            } catch (Exception e) {
                // 인증 실패 시 예외 처리
                log.warn("[WS] CONNECT auth fail: {}", e.getMessage());
            }
            // 헤더 업데이트 반영해 재빌드 필수
            return MessageBuilder.createMessage(message.getPayload(), acc.getMessageHeaders());
        }

        return message;
    }
}
