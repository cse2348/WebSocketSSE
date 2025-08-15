package com.example.WebSocketSSE.repository;

import com.example.WebSocketSSE.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> { // User 엔티티용 JPA 리포지토리
    Optional<User> findByUsername(String username); // username으로 사용자 조회 (Optional 반환)
    boolean existsByUsername(String username); // username 존재 여부 확인
}
