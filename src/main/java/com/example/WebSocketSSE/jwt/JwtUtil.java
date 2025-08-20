package com.example.WebSocketSSE.jwt;

import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.repository.UserRepository;
import com.example.WebSocketSSE.entity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;

// JWT 생성/검증 + Spring Security Authentication 생성 유틸
@Component
public class JwtUtil {

    private static final Duration DEFAULT_EXP = Duration.ofHours(1); // 1시간
    private final SecretKey key;                 // JWT 서명 키
    private final UserRepository userRepository; // DB에서 사용자 로드용

    // @param secret 환경설정의 jwt.secret 값 (Base64 또는 평문 긴 문자열)
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   UserRepository userRepository) {
        this.userRepository = userRepository;

        String cleaned = secret == null ? "" : secret.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("JWT secret is empty. Set jwt.secret / JWT_SECRET.");
        }

        // 1) Base64로 보이면 decode, 아니면 평문 바이트 사용
        byte[] keyBytes;
        if (looksLikeBase64(cleaned)) {
            try {
                keyBytes = Decoders.BASE64.decode(cleaned);
            } catch (IllegalArgumentException e) {
                // Base64처럼 보이지만 깨진 값이면 평문으로 폴백
                keyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            keyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
        }

        // 2) HS256 최소 256bit(=32바이트) 권장
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret too short (need >= 32 bytes / 256 bits).");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 3) 가시화: 서버 기동 시 키 지문(앞 8바이트의 SHA-256) 출력
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

    // Base64로 보일 때만 true (길이 4 배수 + 허용문자 + 최대 2개 패딩)
    private boolean looksLikeBase64(String s) {
        if (s.length() % 4 != 0) return false;
        return s.matches("^[A-Za-z0-9+/]+={0,2}$");
    }

    // 토큰 생성 (클레임: sub=userId)
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

    // Bearer 접두사 제거
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

    //userId(Long) 추출 (유효성 포함)
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

    //토큰 유효성만 체크
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    public Authentication getAuthentication(String token) {
        // 1) 토큰에서 userId 추출(검증 포함)
        Long userId = validateAndGetUserId(token);

        // 2) DB에서 사용자 로드 (id 기반)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: id=" + userId));

        // 3) UserPrincipal 구성(권한 포함)
        UserPrincipal principal = UserPrincipal.from(user);

        // 4) Authentication 생성 (principal=UserPrincipal, credentials=null, authorities=principal.getAuthorities())
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // 사용하기 편하도록 userId를 details에 넣어둔다.
        auth.setDetails(userId);

        return auth;
    }

}
