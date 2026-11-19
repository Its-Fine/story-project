package com.example.story.configuration;

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
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "story")
@Validated
@Getter @Setter
public class StoryProperties {

    public enum Roles {PARTICIPANT, ADMIN}

    @Valid
    private Jwt jwt = new Jwt();
    private Contest contest = new Contest();
    private Application application = new Application();
    private Work work = new Work();
    @Valid
    private Admin admin = new Admin();
    @Valid
    private Minio minio = new Minio();
    @Valid
    private Cors cors = new Cors();
    @Valid
    private Bootstrap bootstrap = new Bootstrap();
    @Valid
    private Oidc oidc = new Oidc();

    @Getter @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        private Duration expirationAccess;
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

    @Getter @Setter
    public static class Contest {
        private int nameSize;
        private int applicationCount;
    }

    @Getter @Setter
    public static class Application {
        private DataSize size;
        private int nameSize;
        private int count;
        private int workCount;
    }

    @Getter @Setter
    public static class Work {
        private DataSize size;
        private int nameSize;
    }

    @Getter @Setter
    public static class Minio {
        @NotBlank
        private String url;
        @NotBlank
        private String accessKey;
        @NotBlank
        private String secretKey;
        @NotBlank
        private String bucket;
    }

    @Getter @Setter
    public static class Admin {
        private String email;
        private String password;
    }

    @Getter @Setter
    public static class Cors {
        @NotEmpty
        private List<@NotBlank String> allowedOrigins = new ArrayList<>();
    }

    @Getter @Setter
    public static class Bootstrap {
        private boolean adminEnabled;
        private boolean contestsEnabled;
        @NotBlank
        private String contestsResource = "classpath:bootstrap/contests.json";
    }

    @Getter @Setter
    public static class Oidc {
        private boolean enabled;
        private String issuerUri;
        private String jwkSetUri;
        private String audience;

        @AssertTrue(message = "story.oidc issuer-uri, jwk-set-uri and audience are required when OIDC is enabled")
        public boolean isConfigurationValid() {
            return !enabled || (
                    hasText(issuerUri)
                    && hasText(jwkSetUri)
                    && hasText(audience)
            );
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
