package com.example.WebSocketSSE.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
// 사용자 ID
    private String token;
}
