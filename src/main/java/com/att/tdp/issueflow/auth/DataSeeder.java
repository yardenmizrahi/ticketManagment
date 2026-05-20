package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.users.Role;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${issueflow.default-password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin@example.com", "Admin User", Role.ADMIN);
        createIfMissing("dev", "dev@example.com", "Developer User", Role.DEVELOPER);
    }

    private void createIfMissing(String username, String email, String fullName, Role role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        userRepository.save(user);
    }
}
