package com.example.WebSocketSSE.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key; // JWT 서명 키

    public JwtUtil(@Value("${JWT_SECRET}") String secret) { // 환경변수에서 JWT_SECRET 읽어오기
        this.key = Keys.hmacShaKeyFor(secret.getBytes()); // 서명 키 생성
    }

    // JWT 생성
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 사용자 ID를 subject에 저장
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // 만료 시간(1시간)
                .signWith(key, SignatureAlgorithm.HS256) // HMAC-SHA256 서명
                .compact(); // 최종 문자열 형태로 변환
    }

    // 토큰 검증 (유효하면 true, 아니면 false)
    public boolean validateToken(String token) {
        try {
            parse(token); // parse() 내부에서 만료/서명 검증 수행
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // JWT에서 userId(subject) 추출
    public Long validateAndGetUserId(String token) {
        return Long.valueOf(parse(token).getSubject()); // 토큰 파싱 후 subject 반환
    }

    // 토큰 파싱 → Claims 반환
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key) // 서명 키 설정
                .build()
                .parseClaimsJws(token) // JWT 파싱 및 서명 검증
                .getBody(); // Claims 반환
    }

    // JWT 기반 Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        Long userId = validateAndGetUserId(token);

        // Spring Security UserDetails 생성
        UserDetails userDetails = User.builder()
                .username(String.valueOf(userId))
                .password("") // JWT 기반 인증이라 비밀번호 불필요
                .authorities(Collections.emptyList())
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
