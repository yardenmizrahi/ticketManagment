package com.att.tdp.issueflow.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserDtos {
    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank String fullName,
            @NotNull Role role,
            String password
    ) {
    }

    public record UpdateUserRequest(String fullName, Role role) {
    }

    public record UserResponse(Long id, String username, String email, String fullName, Role role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }
}
