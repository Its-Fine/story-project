package com.example.story.auth;

import com.example.story.auth.entity.AuthUser;
import com.example.story.auth.repository.AuthUserRepository;
import com.example.story.auth.security.JwtService;
import com.example.story.testsupport.JwtTokens;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AuthUserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    AuthUser user;
    String access;
    String refresh;
    private static final String LOGIN = "{\"email\":\"security-test@example.com\",\"password\":\"Password123!\"}";
    private static final String RESET = "{\"current_password\":\"Password123!\",\"new_password\":\"NewPassword123!\"}";

    @BeforeEach
    void createUser() {
        users.deleteAll();
        user = new AuthUser();
        user.setEmail(JwtTokens.EMAIL);
        user.setPassword(passwords.encode("Password123!"));
        user.setFirstName("Security");
        user.setLastName("Test");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user = users.saveAndFlush(user);
        access = jwt.generateAccessToken(user);
        refresh = jwt.generateRefreshToken(user);
    }

    @Test
    void meAcceptsAccessButRejectsRefreshAndMissingCredentials() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(JwtTokens.EMAIL))
                .andExpect(jsonPath("role").value("PARTICIPANT"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/auth/reset").contentType(MediaType.APPLICATION_JSON).content(RESET))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("com.example.story.testsupport.JwtTokens#invalidTokens")
    void malformedTokensNeverReachProtectedControllers(String token) throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/auth/reset").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(RESET)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", token == null ? "" : token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshOnlyAcceptsRefreshCookie() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String renewed = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("access_token").asText();
        assertThat(jwt.isAccessTokenValid(renewed)).isTrue();
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", access))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void banningAnExistingSessionBlocksMeResetRefreshAndLogin() throws Exception {
        user.setBanned(true);
        users.saveAndFlush(user);
        assertSessionRejected();
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized());
        assertThat(passwords.matches("Password123!", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
        user.setBanned(false);
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
    }

    @Test
    void deletedAccountCannotUseExistingTokens() throws Exception {
        users.deleteById(user.getId());
        assertSessionRejected();
    }

    private void assertSessionRejected() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/auth/reset").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content(RESET)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void meAndRefreshUseCurrentDatabaseRole() throws Exception {
        user.setRole("ADMIN");
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value("ADMIN"));
        String renewed = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(renewed).get("access_token").asText();
        assertThat(io.jsonwebtoken.Jwts.parserBuilder().setSigningKey(JwtTokens.KEY).build()
                .parseClaimsJws(token).getBody().get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void resetRequiresAccessAndCurrentPassword() throws Exception {
        mvc.perform(put("/api/v1/auth/reset").header("Authorization", "Bearer " + refresh)
                .contentType(MediaType.APPLICATION_JSON).content(RESET)).andExpect(status().isUnauthorized());
        for (String body : new String[]{"{\"new_password\":\"NewPassword123!\"}",
                "{\"current_password\":\"wrong\",\"new_password\":\"NewPassword123!\"}",
                "{\"current_password\":\"Password123!\",\"new_password\":\"short\"}"}) {
            mvc.perform(put("/api/v1/auth/reset").header("Authorization", "Bearer " + access)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertThat(passwords.matches("Password123!", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
        mvc.perform(put("/api/v1/auth/reset").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content(RESET)).andExpect(status().isOk());
        assertThat(passwords.matches("NewPassword123!", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void loginAndSignupPreserveFrontendTokenAndCookieContract() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andExpect(jsonPath("access_token").isString())
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/v1/auth/refresh"))
                .andExpect(cookie().sameSite("refreshToken", "Lax"));
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"signup@example.com","password":"Password123!","first_name":"Test",
                 "last_name":"User","birth_date":"2000-01-01","consent":true,"parent_consent":false}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("access_token").isString())
                .andExpect(cookie().httpOnly("refreshToken", true));
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk()).andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    void corsAllowsOnlyConfiguredCredentialedOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
