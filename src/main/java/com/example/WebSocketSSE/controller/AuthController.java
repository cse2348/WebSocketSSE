package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.LoginRequest;
import com.example.WebSocketSSE.dto.LoginResponse;
import com.example.WebSocketSSE.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // REST API 컨트롤러임을 명시 (JSON 형태 응답)
@RequestMapping("/auth") // 기본 URL 경로 /auth
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class AuthController {

    private final AuthService authService; // 인증 서비스 주입

    @PostMapping("/login") // POST /auth/login 요청 처리
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) { // 요청 본문을 LoginRequest로 매핑
        return ResponseEntity.ok(authService.login(request)); // 로그인 처리 후 200 OK 응답과 함께 LoginResponse 반환
    }
}