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

    public JwtUtil(@Value("${JWT_SECRET}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // 토큰 생성
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // userId를 subject에 저장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // 1시간 유효
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            parse(token); // 만료/서명 검증
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // userId(Long) 추출
    public Long validateAndGetUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    // Claims 파싱
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        Long userId = validateAndGetUserId(token);

        UserDetails userDetails = User.withUsername(String.valueOf(userId))
                .password("") // JWT 기반 인증이므로 비번 불필요
                .authorities(Collections.emptyList())
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
