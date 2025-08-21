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

@Component
public class JwtUtil {

    private static final Duration DEFAULT_EXP = Duration.ofHours(1); // 기본 만료 시간: 1시간
    private final SecretKey key; // JWT 서명 키
    private final UserRepository userRepository; // 사용자 조회용 (권한 부여 위해 필요)

    // jwt.secret 값을 받아 SecretKey 초기화
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   UserRepository userRepository) {
        this.userRepository = userRepository;

        String cleaned = secret == null ? "" : secret.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("JWT secret is empty. Set jwt.secret / JWT_SECRET.");
        }

        // Base64로 보이면 decode, 아니면 평문 바이트 사용
        byte[] keyBytes;
        if (looksLikeBase64(cleaned)) {
            try {
                keyBytes = Decoders.BASE64.decode(cleaned);
            } catch (IllegalArgumentException e) {
                keyBytes = cleaned.getBytes(StandardCharsets.UTF_8); // 깨진 값이면 평문으로 처리
            }
        } else {
            keyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
        }

        // HS256은 최소 256bit(32바이트) 이상 필요
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret too short (need >= 32 bytes / 256 bits).");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes); // 최종 SecretKey 생성

        // 서버 기동 시 SecretKey fingerprint 로그 출력 (디버깅용)
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

    // Base64 문자열 판별
    private boolean looksLikeBase64(String s) {
        if (s.length() % 4 != 0) return false;
        return s.matches("^[A-Za-z0-9+/]+={0,2}$");
    }

    // JWT 토큰 생성 (subject=userId)
    public String generateToken(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is null.");
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + DEFAULT_EXP.toMillis());

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // sub에 userId 저장
                .setIssuedAt(iat) // 발급 시간
                .setExpiration(exp) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명
                .compact();
    }

    // "Bearer " 접두사 제거
    private String stripBearer(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return t.substring(7).trim();
        }
        return t;
    }

    // Claims 파싱 (서명 검증 + 만료 검증)
    public Claims parse(String token) {
        String raw = stripBearer(token);
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("JWT token is empty.");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key) // 서명 키 설정
                .build()
                .parseClaimsJws(raw) // 파싱 + 검증
                .getBody();
    }

    // 토큰 검증 후 userId(Long) 추출
    public Long validateAndGetUserId(String token) {
        Claims c = parse(token);
        String sub = c.getSubject(); // sub=userId
        if (sub == null || sub.isEmpty()) {
            throw new JwtException("JWT subject is empty.");
        }
        try {
            return Long.valueOf(sub); // 문자열을 Long으로 변환
        } catch (NumberFormatException e) {
            throw new JwtException("JWT subject is not a valid Long: " + sub);
        }
    }

    // 토큰 유효성만 체크
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true; // 파싱 성공 = 유효
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 만료/위조/형식 오류
        }
    }

    // Authentication 객체 생성 (SecurityContextHolder에 넣을 용도)
    public Authentication getAuthentication(String token) {
        Long userId = validateAndGetUserId(token); // userId 추출 + 검증

        User user = userRepository.findById(userId) // DB에서 사용자 로드
                .orElseThrow(() -> new UsernameNotFoundException("User not found: id=" + userId));

        UserPrincipal principal = UserPrincipal.from(user); // User → UserPrincipal 변환

        //Authentication 생성
        //principal: userId 문자열 (getName() 반환 값) , details  : UserPrincipal (추가정보, 권한 포함)
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, principal.getAuthorities());
        auth.setDetails(principal);

        return auth;
    }
}
