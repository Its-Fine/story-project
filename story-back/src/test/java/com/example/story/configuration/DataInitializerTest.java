package com.example.story.configuration;

import com.example.story.entity.ContestEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.ContestRepository;
import com.example.story.repository.EventSettingsRepository;
import com.example.story.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final ContestRepository contests = mock(ContestRepository.class);
    private final EventSettingsRepository eventSettings = mock(EventSettingsRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final ResourceLoader resources = mock(ResourceLoader.class);
    private final StoryProperties properties = new StoryProperties();
    private final DataInitializer initializer = new DataInitializer(
            users, contests, eventSettings, properties, passwords, new ObjectMapper(), resources);

    @Test
    void normalStartupDoesNotBootstrapAdminOrContests() {
        when(eventSettings.existsById((short) 1)).thenReturn(true);

        initializer.run(mock(ApplicationArguments.class));

        verify(users, never()).findByEmail(any());
        verify(contests, never()).findByName(any());
    }

    @Test
    void existingAdminIsNeverOverwritten() {
        properties.getBootstrap().setAdminEnabled(true);
        properties.getAdmin().setEmail("existing@example.com");
        properties.getAdmin().setPassword("ConfiguredPassword123");
        when(eventSettings.existsById((short) 1)).thenReturn(true);
        when(users.findByEmail("existing@example.com")).thenReturn(Optional.of(new UserEntity()));

        initializer.run(mock(ApplicationArguments.class));

        verify(users, never()).save(any());
        verify(passwords, never()).encode(any());
    }

    @Test
    void enabledContestBootstrapAddsOnlyMissingEntries() {
        properties.getBootstrap().setContestsEnabled(true);
        when(eventSettings.existsById((short) 1)).thenReturn(true);
        String json = """
                [
                  {"name":"Existing","description":"Keep","startAge":10,"endAge":100},
                  {"name":"New","description":"Add","startAge":12,"endAge":90}
                ]
                """;
        when(resources.getResource(any())).thenReturn(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
        when(contests.findByName("Existing")).thenReturn(Optional.of(new ContestEntity()));
        when(contests.findByName("New")).thenReturn(Optional.empty());

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<ContestEntity> saved = ArgumentCaptor.forClass(ContestEntity.class);
        verify(contests).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("New");
    }

    @Test
    void enabledAdminBootstrapRequiresExplicitStrongCredentials() {
        properties.getBootstrap().setAdminEnabled(true);
        properties.getAdmin().setEmail("admin@example.com");
        properties.getAdmin().setPassword("short");
        when(eventSettings.existsById((short) 1)).thenReturn(true);

        assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
    }

    @Test
    void newAdministratorReceivesDatabaseRoleMapping() {
        properties.getBootstrap().setAdminEnabled(true);
        properties.getAdmin().setEmail("admin@example.com");
        properties.getAdmin().setPassword("ConfiguredPassword123");
        when(eventSettings.existsById((short) 1)).thenReturn(true);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });

        initializer.run(mock(ApplicationArguments.class));

        verify(users).assignRole(7L, "ADMIN");
    }
}
