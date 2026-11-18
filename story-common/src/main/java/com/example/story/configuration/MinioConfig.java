package com.example.story.configuration;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private final StoryProperties storyProperties;

    public MinioConfig(StoryProperties storyProperties) {
        this.storyProperties = storyProperties;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storyProperties.getMinio().getUrl())
                .credentials(storyProperties.getMinio().getAccessKey(), storyProperties.getMinio().getSecretKey())
                .build();
    }
}
