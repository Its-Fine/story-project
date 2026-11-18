package com.example.story.configuration;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryPropertiesTest {

    @Test
    void rejectsInvalidJwtAndMissingStorageCredentials() {
        StoryProperties properties = new StoryProperties();
        properties.getJwt().setSecret("not-base64");
        properties.getCors().setAllowedOrigins(List.of("http://localhost:3000"));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var errors = factory.getValidator().validate(properties);
            assertThat(errors).anyMatch(error -> error.getMessage().contains("decode to at least 32 bytes"));
            assertThat(errors).anyMatch(error -> error.getPropertyPath().toString().startsWith("minio."));
        }
    }
}
