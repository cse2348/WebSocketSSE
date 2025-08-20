package com.example.WebSocketSSE.entity;

import com.example.WebSocketSSE.entity.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password; // 이미 인코딩된 해시. 필요 없으면 ""로
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public static UserPrincipal from(User u) {
        String role = u.getRole(); // 예: "ROLE_USER"
        List<GrantedAuthority> auth =
                (role == null || role.isBlank()) ? List.of()
                        : List.of(new SimpleGrantedAuthority(role));

        return UserPrincipal.builder()
                .id(u.getId())
                .username(u.getUsername())
                .password(u.getPassword() == null ? "" : u.getPassword())
                .authorities(auth)
                .enabled(true)
                .build();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
