package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.LoginRequest;
import com.example.WebSocketSSE.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        String token = authService.login(req.getUsername(), req.getPassword());
        return Map.of("accessToken", token);
    }
}
