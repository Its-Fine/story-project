package com.example.story.auth.security;

import com.example.story.auth.config.StoryAuthProperties;
import com.example.story.auth.entity.AuthUser;
import com.example.story.testsupport.JwtTokens;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(properties());

    private static StoryAuthProperties properties() {
        var properties = new StoryAuthProperties();
        properties.getJwt().setSecret(JwtTokens.SECRET);
        properties.getJwt().setExpirationAccess(Duration.ofMinutes(15));
        properties.getJwt().setExpirationRefresh(Duration.ofDays(1));
        return properties;
    }

    @Test
    void issuedTokensHaveSeparatePurposesAndLifetimes() {
        var user = new AuthUser();
        user.setEmail(JwtTokens.EMAIL);
        user.setRole("PARTICIPANT");
        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user);

        assertThat(jwt.isAccessTokenValid(access)).isTrue();
        assertThat(jwt.isRefreshTokenValid(access)).isFalse();
        assertThat(jwt.isAccessTokenValid(refresh)).isFalse();
        assertThat(jwt.isRefreshTokenValid(refresh)).isTrue();
        assertThat(jwt.extractAccessTokenSubject(access)).isEqualTo(JwtTokens.EMAIL);
        assertThat(jwt.extractRefreshTokenSubject(refresh)).isEqualTo(JwtTokens.EMAIL);
        var parser = Jwts.parserBuilder().setSigningKey(JwtTokens.KEY).build();
        var accessClaims = parser.parseClaimsJws(access).getBody();
        var refreshClaims = parser.parseClaimsJws(refresh).getBody();
        assertThat(accessClaims.getExpiration().getTime() - accessClaims.getIssuedAt().getTime()).isEqualTo(900_000L);
        assertThat(refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime()).isEqualTo(86_400_000L);
    }

    @ParameterizedTest
    @MethodSource("com.example.story.testsupport.JwtTokens#invalidTokens")
    void invalidClaimsOrSignaturesAreRejected(String token) {
        assertThat(jwt.isAccessTokenValid(token)).isFalse();
        assertThat(jwt.isRefreshTokenValid(token)).isFalse();
        assertThatThrownBy(() -> jwt.extractAccessTokenSubject(token)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jwt.extractRefreshTokenSubject(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void roleClaimIsNotNeededToIdentifyTheAccount() {
        assertThat(jwt.extractAccessTokenSubject(JwtTokens.token("ACCESS", null))).isEqualTo(JwtTokens.EMAIL);
    }
}
