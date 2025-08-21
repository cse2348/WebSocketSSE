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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // 스프링 설정 클래스임을 표시
@EnableWebSecurity // Spring Security 활성화
@RequiredArgsConstructor // 생성자 주입 자동 생성 (final 필드 대상)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 인증 필터

    private final AuthenticationEntryPoint restAuthenticationEntryPoint; // 인증 실패 처리 (401)
    private final AccessDeniedHandler restAccessDeniedHandler; // 인가 실패 처리 (403)

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화용 Bean 등록
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (JWT 사용하므로 불필요)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함 (STATELESS)

                .exceptionHandling(ex -> ex // 인증/인가 예외 처리 설정
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // 401 응답 처리
                        .accessDeniedHandler(restAccessDeniedHandler) // 403 응답 처리
                )

                .authorizeHttpRequests(auth -> auth // URL별 접근 권한 설정
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS 프리플라이트 요청 허용
                        .requestMatchers("/ws/**").permitAll() // WebSocket 연결은 인증 없이 허용
                        .requestMatchers("/auth/**", "/health").permitAll() // 로그인/회원가입, 헬스체크는 허용
                        .requestMatchers(HttpMethod.GET, "/chat/history/**").permitAll() // 채팅 기록 조회는 허용
                        .requestMatchers("/sse/subscribe", "/sse/history", "/sse/notify").authenticated() // SSE API는 인증 필요
                        .anyRequest().authenticated() // 나머지는 인증 필요
                )

                .formLogin(f -> f.disable()) // 폼 로그인 비활성화
                .httpBasic(b -> b.disable()); // HTTP Basic 인증 비활성화

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록

        return http.build(); // SecurityFilterChain 객체 반환
    }
}
