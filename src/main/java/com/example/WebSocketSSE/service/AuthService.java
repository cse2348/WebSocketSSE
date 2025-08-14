package com.example.WebSocketSSE.service;

import com.example.WebSocketSSE.dto.LoginRequest;
import com.example.WebSocketSSE.dto.LoginResponse;
import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.jwt.JwtUtil;
import com.example.WebSocketSSE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token);
    }
}
