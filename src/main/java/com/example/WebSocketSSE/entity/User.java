package com.example.WebSocketSSE.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id // 기본 키 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 자동 증가 (IDENTITY 전략)
    private Long id; // 사용자 ID

    @Column(nullable = false, unique = true) // NULL 불가 + 유니크 제약 조건
    private String username; // 사용자 이름(로그인 ID)

    @Column(nullable = false) // NULL 불가
    private String password; // 비밀번호 (암호화 저장)

    @Column(nullable = false) // NULL 불가
    private String role; // 사용자 역할 (예: ROLE_USER, ROLE_ADMIN)

    // 정적 팩토리 메서드: User 객체를 생성하는 편의 메서드
    public static User of(String username, String password, String role) {
        return User.builder()
                .username(username) // 사용자 이름 설정
                .password(password) // 비밀번호 설정
                .role(role) // 역할 설정
                .build(); // User 객체 생성
    }
}
