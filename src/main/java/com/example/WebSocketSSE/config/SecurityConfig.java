package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtAuthenticationFilter;
import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil; // JWT 토큰 유틸리티 주입

    // 비밀번호 암호화용 Bean (BCrypt 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    // HTTP 보안 필터 체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 기반이므로 불필요)
                .csrf(csrf -> csrf.disable())
                // 세션 사용 안 함 (STATELESS)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 엔드포인트 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // WebSocket 핸드셰이크 & 로그인 & 헬스체크는 공개
                        .requestMatchers("/ws/**", "/auth/**", "/health").permitAll()
                        // 채팅 히스토리 조회는 공개 (GET만 허용)
                        .requestMatchers(HttpMethod.GET, "/chat/history/**").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
