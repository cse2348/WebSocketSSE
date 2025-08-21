package com.example.WebSocketSSE.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
// Spring Security에서 사용할 사용자 인증 객체
public class UserPrincipal implements UserDetails {

    private final Long id; // 사용자 PK
    private final String username; // 로그인 ID (고유 값)
    private final String password; // 인코딩된 비밀번호 (불필요하면 빈 문자열 가능)
    private final Collection<? extends GrantedAuthority> authorities; // 권한 목록
    private final boolean enabled; // 활성화 여부

    // User 엔티티를 UserPrincipal 로 변환하는 정적 메서드
    public static UserPrincipal from(User u) {
        String role = u.getRole(); // User 엔티티의 권한 값 (예: "ROLE_USER")
        List<GrantedAuthority> auth =
                (role == null || role.isBlank()) ? List.of() // 권한 없으면 빈 리스트
                        : List.of(new SimpleGrantedAuthority(role)); // 권한 문자열을 Security 객체로 변환

        return UserPrincipal.builder()
                .id(u.getId()) // DB PK
                .username(u.getUsername()) // username
                .password(u.getPassword() == null ? "" : u.getPassword()) // password (null이면 "")
                .authorities(auth) // 권한 목록
                .enabled(true) // 기본 활성화
                .build();
    }

    @Override public boolean isAccountNonExpired() { return true; } // 계정 만료 여부 (기본 true)
    @Override public boolean isAccountNonLocked() { return true; } // 계정 잠금 여부 (기본 true)
    @Override public boolean isCredentialsNonExpired() { return true; } // 비밀번호 만료 여부 (기본 true)
    @Override public boolean isEnabled() { return enabled; } // 계정 활성화 여부
}
