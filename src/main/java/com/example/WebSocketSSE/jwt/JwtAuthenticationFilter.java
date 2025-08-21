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
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 요청마다 실행되는 JWT 필터

    private final JwtUtil jwtUtil; // JWT 유틸리티 클래스

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI(); // 요청 URI
        String method = request.getMethod(); // 요청 메서드

        // WebSocket 핸드셰이크(/ws/ 경로) 및 CORS 프리플라이트(OPTIONS)는 JWT 검사 생략
        if ("OPTIONS".equalsIgnoreCase(method) || uri.startsWith("/ws/")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request); // Authorization 헤더에서 토큰 추출
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) { // 토큰이 존재하고 유효하면
                Authentication auth = jwtUtil.getAuthentication(token); // 토큰으로 Authentication 생성 (UserPrincipal)

                if (auth instanceof AbstractAuthenticationToken aat) {
                    // 요청 세부 정보(IP, 세션 등)를 Authentication 객체에 저장
                    aat.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // SecurityContextHolder에 인증 정보 저장 → 이후 컨트롤러에서 @AuthenticationPrincipal 접근 가능
                    SecurityContextHolder.getContext().setAuthentication(aat);
                } else {
                    // 다른 Authentication 구현체여도 그냥 SecurityContext에 저장
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // 토큰 위조/만료/파싱 오류 시 인증은 무효화하지만 요청 자체는 계속 진행
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response); // 다음 필터로 진행
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization"); // Authorization 헤더
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7); // "Bearer " 제거 후 토큰 반환
        }
        return null; // 없거나 형식이 잘못된 경우 null
    }
}
