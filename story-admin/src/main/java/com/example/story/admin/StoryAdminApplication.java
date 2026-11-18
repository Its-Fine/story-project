package com.example.story.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.story")
@EntityScan("com.example.story.entity")
@EnableJpaRepositories("com.example.story.repository")
public class StoryAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoryAdminApplication.class, args);
    }
}
