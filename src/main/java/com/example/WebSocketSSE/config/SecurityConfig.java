package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtAuthenticationFilter;
import com.example.WebSocketSSE.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Spring 설정 클래스 지정
@RequiredArgsConstructor // final 필드를 포함한 생성자 자동 생성
public class SecurityConfig {

    private final JwtUtil jwtUtil; // JWT 토큰 관련 기능 제공 객체

    @Bean // Spring Bean 등록
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보안 기능 비활성화 (JWT 사용 시 불필요)
                .cors(Customizer.withDefaults()) // CORS 기본 설정 적용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안 함 (STATELESS)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/signup", "/health").permitAll() // 로그인, 회원가입, 헬스체크는 인증 없이 접근 허용
                        .requestMatchers("/ws/chat/**").permitAll() // WebSocket 채팅 관련 요청 모두 허용
                        .requestMatchers(HttpMethod.GET, "/chat/history/**").authenticated() // 채팅 기록 조회는 인증 필요
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class); // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가

        return http.build(); // SecurityFilterChain 객체 반환
    }
}