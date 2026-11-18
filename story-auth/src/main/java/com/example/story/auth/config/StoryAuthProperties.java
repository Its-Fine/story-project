package com.example.story.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "story")
@Validated
@Getter
@Setter
public class StoryAuthProperties {

    @Valid
    private Jwt jwt = new Jwt();
    @Valid
    private Cookie cookie = new Cookie();
    @Valid
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        @NotNull
        private Duration expirationAccess;
        @NotNull
        private Duration expirationRefresh;

        @AssertTrue(message = "story.jwt.secret must be Base64 and decode to at least 32 bytes")
        public boolean isSecretValid() {
            if (secret == null || secret.isBlank()) {
                return true;
            }
            try {
                return Base64.getDecoder().decode(secret).length >= 32;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }

    @Getter
    @Setter
    public static class Cookie {
        @NotNull
        private Boolean secure;
        @NotBlank
        private String sameSite;

        @AssertTrue(message = "story.cookie.same-site must be Strict, Lax or None; None requires Secure=true")
        public boolean isSameSiteValid() {
            if (sameSite == null || sameSite.isBlank()) {
                return true;
            }
            boolean supported = List.of("Strict", "Lax", "None").stream()
                    .anyMatch(value -> value.equalsIgnoreCase(sameSite));
            return supported && (!"None".equalsIgnoreCase(sameSite) || Boolean.TRUE.equals(secure));
        }
    }

    @Getter
    @Setter
    public static class Cors {
        @NotEmpty
        private List<@NotBlank String> allowedOrigins = new ArrayList<>();
    }
}
