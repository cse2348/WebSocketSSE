package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SseConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/sse/**") // SSE 엔드포인트에 대해 CORS 허용
                .allowedMethods("GET","POST") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 요청 헤더 허용
                .exposedHeaders("Content-Type") // 클라이언트에 노출할 헤더
                .allowCredentials(true) // 쿠키/인증정보 포함 허용
                .maxAge(3600); // CORS preflight 결과 캐싱 시간(초)
    }
}
