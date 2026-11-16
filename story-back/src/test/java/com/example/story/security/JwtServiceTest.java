package com.example.story.security;

import com.example.story.configuration.StoryProperties;
import com.example.story.testsupport.JwtTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(properties());

    private static StoryProperties properties() {
        var properties = new StoryProperties();
        properties.getJwt().setSecret(JwtTokens.SECRET);
        return properties;
    }

    @Test
    void onlyAccessTokensAreAccepted() {
        String access = JwtTokens.token("ACCESS", "PARTICIPANT");
        String refresh = JwtTokens.token("REFRESH", "PARTICIPANT");
        assertThat(jwt.isTokenValid(access)).isTrue();
        assertThat(jwt.extractAccessTokenSubject(access)).isEqualTo(JwtTokens.EMAIL);
        assertThat(jwt.isTokenValid(refresh)).isFalse();
        assertThatThrownBy(() -> jwt.extractAccessTokenSubject(refresh)).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @MethodSource("com.example.story.testsupport.JwtTokens#invalidTokens")
    void invalidClaimsOrSignaturesAreRejected(String token) {
        assertThat(jwt.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> jwt.extractAccessTokenSubject(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void roleClaimIsNotNeededToIdentifyTheAccount() {
        assertThat(jwt.extractAccessTokenSubject(JwtTokens.token("ACCESS", null))).isEqualTo(JwtTokens.EMAIL);
    }
}
