package com.example.story;

import com.example.story.entity.EventSettingsEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.EventSettingsRepository;
import com.example.story.repository.UserRepository;
import com.example.story.testsupport.JwtTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EventSettingsRepository eventSettings;
    UserEntity user;
    String access;

    @BeforeEach
    void createUser() {
        EventSettingsEntity settings = eventSettings.findById((short) 1).orElseGet(() -> {
            EventSettingsEntity entity = new EventSettingsEntity();
            entity.setId((short) 1);
            return entity;
        });
        settings.setApplicationsClosed(false);
        eventSettings.saveAndFlush(settings);

        users.findByEmail(JwtTokens.EMAIL).ifPresent(users::delete);
        user = new UserEntity();
        user.setEmail(JwtTokens.EMAIL);
        user.setPassword("unused-hash");
        user.setFirstName("Security");
        user.setLastName("Test");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setConsent(true);
        user.setParentConsent(false);
        user = users.saveAndFlush(user);
        access = JwtTokens.token("ACCESS", "PARTICIPANT");
    }

    @Test
    void accountAcceptsAccessButRejectsRefreshAndMissingCredentials() throws Exception {
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(JwtTokens.EMAIL));
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + JwtTokens.token("REFRESH", "ADMIN")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/account/data")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("com.example.story.testsupport.JwtTokens#invalidTokens")
    void invalidTokensReturn401InsteadOf500(String token) throws Exception {
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void banningAndUnbanningAffectsAlreadyIssuedTokens() throws Exception {
        user.setBanned(true);
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        user.setBanned(false);
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
    }

    @Test
    void deletedAccountsAreRejected() throws Exception {
        users.delete(user);
        mvc.perform(get("/api/v1/account/data").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPermissionFollowsDatabaseRoleEvenWithAnOldToken() throws Exception {
        String adminToken = JwtTokens.token("ACCESS", "ADMIN");
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
        user.setRole("ADMIN");
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        user.setRole("PARTICIPANT");
        users.saveAndFlush(user);
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanPersistGlobalClosureAndClosedEventRejectsNewApplications() throws Exception {
        mvc.perform(get("/api/v1/event/status").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.close").value(false));

        user.setRole("ADMIN");
        user.setClosedAccess(true);
        users.saveAndFlush(user);

        mvc.perform(put("/api/v1/admin/event/status")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"close\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.close").value(true));

        assertThat(eventSettings.findById((short) 1))
                .isPresent()
                .get()
                .extracting(EventSettingsEntity::getApplicationsClosed)
                .isEqualTo(true);

        mvc.perform(get("/api/v1/event/status").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.close").value(true));

        user.setRole("PARTICIPANT");
        users.saveAndFlush(user);

        mvc.perform(post("/api/v1/applications")
                        .param("conId", "1")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Closed event application",
                                  "description": "Must not be created",
                                  "city": "Moscow",
                                  "members": [],
                                  "isTeam": false,
                                  "isCurator": false
                                }
                                """))
                .andExpect(status().isGone());
    }
}
