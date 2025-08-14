package com.example.WebSocketSSE.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${JWT_SECRET}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // JWT 생성
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // 1시간
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // token에서 userId 추출 (JwtAuthenticationFilter용)
    public Long validateAndGetUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    // token 파싱해서 Claims 반환 (StompAuthChannelInterceptor용)
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
