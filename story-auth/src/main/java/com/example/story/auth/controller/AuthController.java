package com.example.story.auth.controller;

import com.example.story.auth.dto.AuthRequestDto;
import com.example.story.auth.dto.AuthResponseDto;
import com.example.story.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/signup")
    public ResponseEntity<?> postSignup(@RequestBody @Validated AuthRequestDto request) {
        try {
            Map<String, String> tokens = authService.signup(request);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.get("refresh_token"))
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/v1/auth/refresh")
                    .maxAge(Duration.ofDays(1))
                    .sameSite("None")
                    .build();

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

    @PostMapping("/login")
    public ResponseEntity<?> postLogin(@RequestBody @Validated AuthRequestDto request) {
        try {
            Map<String, String> tokens = authService.login(request);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.get("refresh_token"))
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/v1/auth/refresh")
                    .maxAge(Duration.ofDays(1))
                    .sameSite("None")
                    .build();

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

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("Invalid token format"));
            }
            
            String token = authHeader.substring(7);
            var userDetails = (org.springframework.security.core.userdetails.User) authService.detailsFromToken(token);
            
            // Извлекаем роль из полномочий (убираем префикс "ROLE_")
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
