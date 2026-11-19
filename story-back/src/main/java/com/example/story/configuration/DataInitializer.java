package com.example.story.configuration;

import com.example.story.entity.ContestEntity;
import com.example.story.entity.EventSettingsEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.ContestRepository;
import com.example.story.repository.EventSettingsRepository;
import com.example.story.repository.UserRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@DependsOn("entityManagerFactory")
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ContestRepository contestRepository;
    private final EventSettingsRepository eventSettingsRepository;
    private final StoryProperties storyProperties;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createEventSettingsIfNotExists();
        if (storyProperties.getBootstrap().isAdminEnabled()) {
            createAdminIfNotExists();
        }
        if (storyProperties.getBootstrap().isContestsEnabled()) {
            importContests();
        }
    }

    private void createAdminIfNotExists() {
        String email = storyProperties.getAdmin().getEmail();
        String password = storyProperties.getAdmin().getPassword();
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new IllegalStateException("STORY_ADMIN_USERNAME must contain a valid email when admin bootstrap is enabled");
        }
        if (!StringUtils.hasText(password) || password.length() < 12) {
            throw new IllegalStateException("STORY_ADMIN_PASSWORD must contain at least 12 characters when admin bootstrap is enabled");
        }

        boolean adminExists = userRepository.findByEmail(email).isPresent();

        if (!adminExists) {
            UserEntity admin = new UserEntity();
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(StoryProperties.Roles.ADMIN.toString());
            admin.setClosedAccess(true);
            admin.setBanned(false);
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setBirthDate(LocalDate.of(2001, 11, 4));
            admin.setConsent(true);
            admin.setParentConsent(true);

            userRepository.saveAndFlush(admin);
            userRepository.assignRole(admin.getId(), StoryProperties.Roles.ADMIN.toString());
            log.info("Initial administrator created for configured email");
        } else {
            log.info("Initial administrator already exists; profile, role and password were not changed");
        }
    }

    private void createEventSettingsIfNotExists() {
        if (eventSettingsRepository.existsById((short) 1)) {
            return;
        }

        EventSettingsEntity settings = new EventSettingsEntity();
        settings.setId((short) 1);
        settings.setApplicationsClosed(false);
        eventSettingsRepository.save(settings);
    }

    private void importContests() {
        Resource resource = resourceLoader.getResource(storyProperties.getBootstrap().getContestsResource());
        List<ContestSeed> newContests;
        try (var inputStream = resource.getInputStream()) {
            newContests = objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read contest bootstrap resource", exception);
        }

        for (ContestSeed newContest : newContests) {
            Optional<ContestEntity> entity = contestRepository.findByName(newContest.name());
            if (entity.isEmpty()) {
                ContestEntity newEntity = new ContestEntity();
                newEntity.setName(newContest.name());
                newEntity.setDescription(newContest.description());
                newEntity.setStartAge(newContest.startAge());
                newEntity.setEndAge(newContest.endAge());
                contestRepository.save(newEntity);
            }
        }
        log.info("Explicit contest bootstrap completed from configured resource");
    }

    public record ContestSeed(String name, String description, Integer startAge, Integer endAge) {}
}
