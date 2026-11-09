package com.example.story.auth.service;

import com.example.story.auth.dto.AuthRequestDto;
import com.example.story.auth.dto.AuthResponseDto;
import com.example.story.auth.entity.AuthUser;
import com.example.story.auth.repository.AuthUserRepository;
import com.example.story.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public Map<String, String> signup(AuthRequestDto dto) {
        if (!Boolean.TRUE.equals(dto.getConsent())) {
            throw new RuntimeException("Consent is required");
        }

        int age = Period.between(dto.getBirthDate(), LocalDate.now()).getYears();
        if (age < 18 && !Boolean.TRUE.equals(dto.getParentConsent())) {
            throw new RuntimeException("For users under 18, parental consent is required");
        }

        if (authUserRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        AuthUser user = new AuthUser();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setBirthDate(dto.getBirthDate());
        user.setRole("PARTICIPANT");
        user.setParentConsent(Boolean.TRUE.equals(dto.getParentConsent()));

        authUserRepository.save(user);

        return Map.of(
                "access_token", jwtService.generateAccessToken(user),
                "refresh_token", jwtService.generateRefreshToken(user)
        );
    }

    public Map<String, String> login(AuthRequestDto dto) {
        AuthUser user = authUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new RuntimeException("User is banned");
        }

        return Map.of(
                "access_token", jwtService.generateAccessToken(user),
                "refresh_token", jwtService.generateRefreshToken(user)
        );
    }

    public String refresh(String refreshToken) {
        org.springframework.security.core.userdetails.UserDetails details = jwtService.detailsFromToken(refreshToken);

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        AuthUser user = authUserRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jwtService.generateAccessToken(user);
    }
    public UserDetails detailsFromToken(String token) {
        return jwtService.detailsFromToken(token);
    }
}
