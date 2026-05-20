package com.att.tdp.issueflow.users;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.common.Require;
import com.att.tdp.issueflow.users.UserDtos.CreateUserRequest;
import com.att.tdp.issueflow.users.UserDtos.UpdateUserRequest;
import com.att.tdp.issueflow.users.UserDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${issueflow.default-password}")
    private String defaultPassword;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(load(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, User actor) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(
                request.password() == null || request.password().isBlank() ? defaultPassword : request.password()));
        User saved = userRepository.save(user);
        auditService.user("CREATE", "USER", saved.getId(), actor);
        return UserResponse.from(saved);
    }

    @Transactional
    public void update(Long id, UpdateUserRequest request, User actor) {
        User user = load(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        auditService.user("UPDATE", "USER", user.getId(), actor);
    }

    @Transactional
    public void delete(Long id, User actor) {
        User user = load(id);
        userRepository.delete(user);
        auditService.user("DELETE", "USER", id, actor);
    }

    public User load(Long id) {
        return Require.found(userRepository.findById(id), "User not found");
    }
}
