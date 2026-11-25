package com.example.story.auth.controller;

import com.example.story.auth.dto.LoginRequestDto;
import com.example.story.auth.dto.SignupRequestDto;
import com.example.story.auth.dto.AuthResponseDto;
import com.example.story.auth.config.StoryAuthProperties;
import com.example.story.auth.entity.AuthUser;
import com.example.story.auth.repository.AuthUserRepository;
import com.example.story.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoryAuthProperties storyAuthProperties;

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(storyAuthProperties.getCookie().getSecure()))
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(1))
                .sameSite(storyAuthProperties.getCookie().getSameSite())
                .build();
    }

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(storyAuthProperties.getCookie().getSecure()))
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite(storyAuthProperties.getCookie().getSameSite())
                .build();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> postSignup(@RequestBody @Validated SignupRequestDto request) {
        try {
            Map<String, String> tokens = authService.signup(request);
            ResponseCookie refreshCookie = buildRefreshCookie(tokens.get("refresh_token"));
            AuthResponseDto result = new AuthResponseDto();
            result.setAccessToken(tokens.get("access_token"));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(result);
        } catch (Exception e) {
            HttpStatus status = "User already exists".equals(e.getMessage())
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(new ErrorDto(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> postLogin(@RequestBody @Validated LoginRequestDto request) {
        try {
            Map<String, String> tokens = authService.login(request);
            ResponseCookie refreshCookie = buildRefreshCookie(tokens.get("refresh_token"));
            AuthResponseDto result = new AuthResponseDto();
            result.setAccessToken(tokens.get("access_token"));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorDto(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> postRefresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        try {
            AuthResponseDto result = new AuthResponseDto();
            result.setAccessToken(authService.refresh(refreshToken));
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorDto(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> postLogout() {
        ResponseCookie clearCookie = buildClearCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "logged out"));
    }

    @PutMapping("/reset")
    public ResponseEntity<?> putReset(
            Authentication authentication,
            @RequestBody Map<String, String> body
    ) {
        try {
            AuthUser user = authUserRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (Boolean.TRUE.equals(user.getBanned())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDto("User is banned"));
            }
            String currentPassword = body.get("current_password");
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(new ErrorDto("Wrong current password"));
            }
            String newPassword = body.get("new_password");
            if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 255) {
                return ResponseEntity.badRequest().body(new ErrorDto("Password must be between 8 and 255 characters"));
            }
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(new ErrorDto("New password must differ from current password"));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            authUserRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "password updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        return authUserRepository.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "roles", authUserRepository.findRoleNamesByUserId(user.getId()),
                        "permissions", authUserRepository.findPermissionNamesByUserId(user.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public static class ErrorDto {
        public String message;
        public ErrorDto(String message) {
            this.message = message;
        }
    }
}
