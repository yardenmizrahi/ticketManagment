package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.auth.AuthDtos.LoginRequest;
import com.att.tdp.issueflow.auth.AuthDtos.LoginResponse;
import com.att.tdp.issueflow.auth.AuthDtos.MeResponse;
import com.att.tdp.issueflow.users.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final TokenDenyList tokenDenyList;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUserDetails details = (AppUserDetails) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())).getPrincipal();
        return new LoginResponse(jwtService.createToken(details), "Bearer", jwtService.expirationSeconds());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenDenyList.deny(header.substring(7), jwtService.expirationSeconds());
        }
    }

    @GetMapping("/me")
    public MeResponse me() {
        User user = currentUser.get();
        return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
