package com.example.story.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "story")
@Getter @Setter
public class StoryProperties {

    public enum Roles {PARTICIPANT, ADMIN}

    private Jwt jwt = new Jwt();
    private Contest contest = new Contest();
    private Application application = new Application();
    private Work work = new Work();
    private Admin admin = new Admin();
    private Minio minio = new Minio();

    private Boolean closed;

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private Duration expirationAccess;
        private Duration expirationRefresh;
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
        private String url;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }

    @Getter @Setter
    public static class Admin {
        private String email;
        private String password;
    }
}
