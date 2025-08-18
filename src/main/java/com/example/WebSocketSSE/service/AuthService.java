package com.example.WebSocketSSE.service;

import com.example.WebSocketSSE.dto.LoginRequest;
import com.example.WebSocketSSE.dto.LoginResponse;
import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.jwt.JwtUtil;
import com.example.WebSocketSSE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 필드 포함 생성자 자동 생성
public class AuthService {

    private final UserRepository userRepository; // 사용자 정보 조회/저장 JPA 리포지토리
    private final PasswordEncoder encoder;       // PasswordEncoder 인터페이스 (BCrypt 구현체가 주입됨)
    private final JwtUtil jwtUtil;               // JWT 생성 및 검증 유틸 (스프링이 주입하는 단일 빈)

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) { // 로그인 처리 메서드
        // NPE 방지: 요청 파라미터 기초 검증
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("username/password is required");
        }

        User user = userRepository.findByUsername(request.getUsername()) // username으로 사용자 조회
                .orElseThrow(() -> new RuntimeException("User not found")); // 없으면 예외 발생

        if (!encoder.matches(request.getPassword(), user.getPassword())) { // 비밀번호 검증
            throw new RuntimeException("Invalid password"); // 틀리면 예외 발생
        }

        String token = jwtUtil.generateToken(user.getId()); // JWT 토큰 생성 (전역 동일 JwtUtil 사용)
        return new LoginResponse(token); // 토큰을 포함한 응답 반환
    }
}
