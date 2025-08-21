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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 401 / 403 JSON 응답용
    private final AuthenticationEntryPoint restAuthenticationEntryPoint;
    private final AccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반 (웹소켓 핸드셰이크는 CSRF 제외)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**")
                        .disable()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 시큐리티 예외 응답(JSON 통일)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(restAccessDeniedHandler)           // 403
                )

                // 인가 정책
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS 프리플라이트
                        .requestMatchers("/ws/**").permitAll()                  // 핸드셰이크는 열어둠
                        .requestMatchers("/auth/**", "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/chat/history/**").permitAll()
                        .requestMatchers("/sse/subscribe", "/sse/history", "/sse/notify").authenticated()
                        .anyRequest().authenticated()
                )

                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable());

        // JWT 필터 등록
        //  - JwtAuthenticationFilter 내부에서 /ws/** 는 early-pass 권장
        //    (핸드셰이크를 토큰 없이 통과시키고, STOMP CONNECT에서 인증)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
