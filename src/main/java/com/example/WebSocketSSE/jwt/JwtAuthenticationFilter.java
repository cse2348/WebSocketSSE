package com.example.WebSocketSSE.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.io.IOException;

// 요청마다 한 번 실행되는 JWT 인증 필터
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // WebSocket 핸드셰이크 및 CORS 프리플라이트는 필터를 통과시킴 (STOMP CONNECT에서 인증 처리)
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || uri.startsWith("/ws/")) {
            chain.doFilter(request, response);
            return;
        }

        // Authorization 헤더에서 Bearer 토큰 추출
        try {
            String token = resolveToken(request);
            // 토큰이 존재하고 유효한 경우
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                Authentication auth = jwtUtil.getAuthentication(token); // principal=UserPrincipal

                // setDetails는 AbstractAuthenticationToken 계열에서만 가능
                if (auth instanceof AbstractAuthenticationToken aat) {
                    // 인증 정보에 요청 세부 정보 추가 (IP, 세션 등)
                    aat.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // SecurityContextHolder에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(aat);
                } else {
                    // 혹시 다른 구현체가 오더라도 인증 자체는 세팅
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // 위조/만료/파싱 오류 등: 인증만 실패시키고 요청은 계속 진행
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization"); // Authorization 헤더에서 Bearer 토큰 추출
        // Authorization 헤더가 없거나 Bearer 접두사가 없으면 null 반환
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
