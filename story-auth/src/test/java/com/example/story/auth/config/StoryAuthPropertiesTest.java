package com.example.story.auth.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryAuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void startupFailsClearlyWhenJwtSecretIsMissing() {
        contextRunner.withPropertyValues(
                        "story.jwt.expiration-access=15m",
                        "story.jwt.expiration-refresh=1d",
                        "story.cookie.secure=false",
                        "story.cookie.same-site=Lax",
                        "story.cors.allowed-origins=http://localhost:3000")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("story.jwt.secret");
                });
    }

    @Test
    void rejectsShortOrMalformedJwtSecrets() {
        StoryAuthProperties properties = validProperties();
        properties.getJwt().setSecret("not-base64");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties))
                    .anyMatch(error -> error.getMessage().contains("decode to at least 32 bytes"));
        }
    }

    @Test
    void rejectsSameSiteNoneWithoutSecureCookie() {
        StoryAuthProperties properties = validProperties();
        properties.getCookie().setSameSite("None");
        properties.getCookie().setSecure(false);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties))
                    .anyMatch(error -> error.getMessage().contains("None requires Secure=true"));
        }
    }

    private StoryAuthProperties validProperties() {
        StoryAuthProperties properties = new StoryAuthProperties();
        properties.getJwt().setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.getJwt().setExpirationAccess(Duration.ofMinutes(15));
        properties.getJwt().setExpirationRefresh(Duration.ofDays(1));
        properties.getCookie().setSecure(false);
        properties.getCookie().setSameSite("Lax");
        properties.getCors().setAllowedOrigins(List.of("http://localhost:3000"));
        return properties;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StoryAuthProperties.class)
    static class PropertiesConfiguration {}
}
