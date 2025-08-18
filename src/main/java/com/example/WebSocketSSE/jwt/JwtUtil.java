package com.example.WebSocketSSE.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key; // JWT 서명 키
    // 생성자 주입을 통해 application.properties에서 JWT 비밀 키를 읽어옴
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // 한쪽은 BASE64, 한쪽은 평문이면 또 서명 오류가 나므로 방식도 통일!
        // (이번 프로젝트에선 '평문 UTF-8'을 표준으로 사용)
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성 (sub = userId)
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // userId를 subject에 저장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60)) // 1시간 유효
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Claims 파싱(서명/만료 검증 포함)
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // userId(Long) 추출
    public Long validateAndGetUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    //  토큰 유효성만 체크 (서명/만료 등)
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 userId를 꺼내 그 문자열을 Principal로 넣어 Authentication 생성
    public Authentication getAuthentication(String token) {
        Long userId = validateAndGetUserId(token);
        String principal = String.valueOf(userId); // 컨트롤러에서 그대로 사용(예: @AuthenticationPrincipal String userId)
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.emptyList()
        );
    }
}
