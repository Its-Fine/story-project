package com.example.story.auth.controller;

import com.example.story.auth.dto.LoginRequestDto;
import com.example.story.auth.dto.SignupRequestDto;
import com.example.story.auth.dto.AuthResponseDto;
import com.example.story.auth.entity.AuthUser;
import com.example.story.auth.repository.AuthUserRepository;
import com.example.story.auth.security.JwtService;
import com.example.story.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${story.cookie.secure:false}")
    private boolean cookieSecure;

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(1))
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
    }

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite(cookieSecure ? "None" : "Lax")
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
    public ResponseEntity<?> postRefresh(@CookieValue("refreshToken") String refreshToken) {
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
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("Invalid token format"));
            }
            String token = authHeader.substring(7);
            String email = jwtService.detailsFromToken(token).getUsername();
            AuthUser user = authUserRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String newPassword = body.get("new_password");
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.badRequest().body(new ErrorDto("Password must be at least 8 characters"));
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
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("Invalid token format"));
            }
            String token = authHeader.substring(7);
            var userDetails = (org.springframework.security.core.userdetails.User) authService.detailsFromToken(token);
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("PARTICIPANT");
            Map<String, String> response = new HashMap<>();
            response.put("email", userDetails.getUsername());
            response.put("role", role);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("Invalid or expired token"));
        }
    }

    public static class ErrorDto {
        public String message;
        public ErrorDto(String message) {
            this.message = message;
        }
    }
}
