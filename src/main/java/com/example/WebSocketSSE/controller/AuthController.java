package com.example.WebSocketSSE.controller;

import com.example.WebSocketSSE.dto.LoginRequest;
import com.example.WebSocketSSE.dto.LoginResponse;
import com.example.WebSocketSSE.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
