package com.example.WebSocketSSE;

import com.example.WebSocketSSE.entity.User;
import com.example.WebSocketSSE.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 초기 유저 2명 생성 (비밀번호는 BCrypt로 암호화)
    @Bean
    CommandLineRunner init(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        return args -> {
            if (!userRepository.existsByUsername("user1")) {
                userRepository.save(User.of("user1", encoder.encode("pass1"), "ROLE_USER"));
                System.out.println("초기 유저 생성: user1 / pass1");
            }
            if (!userRepository.existsByUsername("user2")) {
                userRepository.save(User.of("user2", encoder.encode("pass2"), "ROLE_USER"));
                System.out.println("초기 유저 생성: user2 / pass2");
            }
        };
    }
}
