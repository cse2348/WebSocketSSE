package com.example.WebSocketSSE.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SseConfig implements WebMvcConfigurer {
    // 브라우저서 테스트할 경우 CORS 허용 필요 시 사용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/sse/**")
                .allowedMethods("GET","POST")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
