package com.example.WebSocketSSE.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    // 사용자 이름
    private String username;
    // 비밀번호
    private String password;
}
