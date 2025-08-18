package com.example.WebSocketSSE.config;

import com.example.WebSocketSSE.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter; // 스프링이 주입하는 필터 빈

    // 비밀번호 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // HTTP 보안 필터 체인
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 기반)
                .csrf(csrf -> csrf.disable())
                // 세션 미사용
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 엔드포인트 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // WebSocket 핸드셰이크, 로그인, 헬스체크 허용
                        .requestMatchers("/ws/**", "/auth/**", "/health").permitAll()
                        // 채팅 히스토리 조회 (GET 허용)
                        .requestMatchers(HttpMethod.GET, "/chat/history/**").permitAll()
                        // 그 외 인증 필요
                        .anyRequest().authenticated()
                )
                // 폼/Basic 비활성화 (JWT만 사용)
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable());

        // 빈으로 등록된 필터를 체인에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
