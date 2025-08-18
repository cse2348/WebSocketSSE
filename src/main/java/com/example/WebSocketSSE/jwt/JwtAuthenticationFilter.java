package com.example.WebSocketSSE.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter { // 요청마다 한 번 실행되는 JWT 인증 필터

    private final JwtUtil jwtUtil; // JWT 토큰 검증 유틸 클래스

    public JwtAuthenticationFilter(JwtUtil jwtUtil) { // 생성자 주입
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request); // 요청 헤더에서 JWT 토큰 추출
        if (token != null) {
            Long userId = jwtUtil.validateAndGetUserId(token); // 토큰 검증 및 사용자 ID 추출
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, null); // 인증 객체 생성 (권한은 null)
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 요청 상세 정보 설정
            SecurityContextHolder.getContext().setAuthentication(auth); // SecurityContext에 인증 정보 저장
        }
        chain.doFilter(request, response); // 다음 필터로 요청 전달
    }

    private String resolveToken(HttpServletRequest request) { // JWT 토큰 추출 메서드
        String bearer = request.getHeader("Authorization"); // Authorization 헤더 값 가져오기
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) { // Bearer 형식인지 확인
            return bearer.substring(7); // "Bearer " 제거 후 토큰 반환
        }
        return null; // 토큰 없으면 null 반환
    }
}

