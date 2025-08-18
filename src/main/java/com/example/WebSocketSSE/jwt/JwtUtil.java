package com.example.WebSocketSSE.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Component
public class JwtUtil {

    private static final Duration DEFAULT_EXP = Duration.ofHours(1); // 1시간
    private final SecretKey key; // JWT 서명 키

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        String cleaned = secret == null ? "" : secret.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("JWT secret is empty. Set jwt.secret / JWT_SECRET.");
        }

        byte[] keyBytes;
        if (looksLikeBase64(cleaned)) {
            // Base64라면 decode
            try {
                keyBytes = Decoders.BASE64.decode(cleaned);
            } catch (IllegalArgumentException e) {
                // Base64 모양인데 깨진 값 → 평문으로 폴백
                keyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            // 평문
            keyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
        }

        // HS256 권장 최소 256bit = 32바이트
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret too short (need >= 32 bytes / 256 bits).");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 가시화: 서버 기동 시 키 지문(앞 8바이트의 SHA-256 헤더) 출력
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getEncoded());
            StringBuilder fp = new StringBuilder();
            for (int i = 0; i < 8 && i < digest.length; i++) {
                fp.append(String.format("%02x", digest[i]));
            }
            System.out.println("[JWT] key fingerprint=" + fp + " (head8)");
        } catch (Exception ignore) { /* no-op */ }
    }

    // Base64로 '보일' 때만 true (길이 4 배수 + 허용문자 + 최대 2개 패딩)
    private boolean looksLikeBase64(String s) {
        if (s.length() % 4 != 0) return false;
        return s.matches("^[A-Za-z0-9+/]+={0,2}$");
    }

    // 토큰 생성 (sub=userId)
    public String generateToken(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is null.");
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + DEFAULT_EXP.toMillis());

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Bearer 접두사 제거 (편의용)
    private String stripBearer(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return t.substring(7).trim();
        }
        return t;
    }

    // Claims 파싱(서명/만료 검증 포함)
    public Claims parse(String token) {
        String raw = stripBearer(token);
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("JWT token is empty.");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(raw)
                .getBody();
    }

    // userId(Long) 추출 (유효성 포함)
    public Long validateAndGetUserId(String token) {
        Claims c = parse(token);
        String sub = c.getSubject();
        if (sub == null || sub.isEmpty()) {
            throw new JwtException("JWT subject is empty.");
        }
        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            throw new JwtException("JWT subject is not a valid Long: " + sub);
        }
    }

    //  토큰 유효성만 체크
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Authentication 구성 (Principal = userId 문자열)
    public Authentication getAuthentication(String token) {
        Long userId = validateAndGetUserId(token);
        String principal = String.valueOf(userId);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.emptyList()
        );
    }

    public SecretKey getKey() {
        return key;
    }
}
