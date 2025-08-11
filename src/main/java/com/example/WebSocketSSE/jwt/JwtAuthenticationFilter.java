package com.example.WebSocketSSE.jwt;

import com.example.WebSocketSSE.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    public JwtAuthenticationFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jws<Claims> jws = jwtUtil.parse(token);
                String username = jws.getBody().getSubject();
                String role = (String) jws.getBody().get("role");
                var authToken = new AbstractAuthenticationToken(List.of(new SimpleGrantedAuthority(role))) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return username; }
                };
                authToken.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ignored) { }
        }
        filterChain.doFilter(request, response);
    }
}
