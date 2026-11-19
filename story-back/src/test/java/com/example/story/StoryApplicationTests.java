package com.example.story;

import com.example.story.entity.ApplicationEntity;
import com.example.story.entity.ContestEntity;
import com.example.story.entity.EventSettingsEntity;
import com.example.story.entity.UserEntity;
import com.example.story.entity.WorkEntity;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.ContestRepository;
import com.example.story.repository.EventSettingsRepository;
import com.example.story.repository.UserRepository;
import com.example.story.repository.WorkRepository;
import com.example.story.testsupport.JwtTokens;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ContestRepository contestRepository;
    @Autowired WorkRepository workRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @PersistenceContext EntityManager entityManager;
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
        users.flush();
        user = new UserEntity();
        user.setEmail(JwtTokens.EMAIL);
        user.setPassword("unused-hash");
        user.setFirstName("Security");
        user.setLastName("Test");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setConsent(true);
        user.setParentConsent(false);
        user = users.saveAndFlush(user);
        users.assignRole(user.getId(), "PARTICIPANT");
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
    @Transactional
    void closedEventDoesNotBlockProfileOrPasswordUpdates() throws Exception {
        EventSettingsEntity settings = eventSettings.findById((short) 1).orElseThrow();
        settings.setApplicationsClosed(true);
        eventSettings.saveAndFlush(settings);

        user.setPassword(passwordEncoder.encode("CurrentPass123"));
        users.saveAndFlush(user);

        mvc.perform(put("/api/v1/account/data")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\",\"lastName\":\"Profile\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(put("/api/v1/account/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPass123\",\"newPassword\":\"NewPass456\"}"))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        UserEntity updated = users.findById(user.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Profile");
        assertThat(passwordEncoder.matches("NewPass456", updated.getPassword())).isTrue();
    }

    @Test
    void adminPermissionFollowsDatabaseRoleEvenWithAnOldToken() throws Exception {
        String adminToken = JwtTokens.token("ACCESS", "ADMIN");
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
        changeRole(user, "ADMIN");
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        changeRole(user, "PARTICIPANT");
        mvc.perform(get("/api/v1/admin/users/count").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanPersistGlobalClosureAndClosedEventRejectsNewApplications() throws Exception {
        mvc.perform(get("/api/v1/event/status").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.close").value(false));

        changeRole(user, "ADMIN");
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

        changeRole(user, "PARTICIPANT");

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
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Приём заявок закрыт. Новые заявки временно не принимаются."));
    }

    @Test
    @Transactional
    void adminCanModerateApplicationAndRateWorkAndParticipantSeesResult() throws Exception {
        ContestEntity contest = contestRepository.findAll().getFirst();

        ApplicationEntity application = new ApplicationEntity();
        application.setName("Moderation test");
        application.setDescription("Application under review");
        application.setCity("Moscow");
        application.setIsTeam(false);
        application.setIsCurator(false);
        application.setUserEntity(user);
        application.setContestEntity(contest);
        application = applicationRepository.saveAndFlush(application);

        WorkEntity work = new WorkEntity();
        work.setName("moderation-test.png");
        work.setFileName("moderation-test-storage-key");
        work.setFileSize(1024);
        work.setApplicationEntity(application);
        work = workRepository.saveAndFlush(work);

        changeRole(user, "ADMIN");
        entityManager.clear();

        mvc.perform(patch("/api/v1/admin/applications/{appId}/status", application.getId())
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"Incomplete description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Incomplete description"))
                .andExpect(jsonPath("$.reviewedBy").value(JwtTokens.EMAIL));

        mvc.perform(put("/api/v1/admin/works/{workId}/rating", work.getId())
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":101}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/admin/works/{workId}/rating", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":87}"))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/v1/admin/works/{workId}/rating", work.getId())
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":87}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(87))
                .andExpect(jsonPath("$.isRated").value(true))
                .andExpect(jsonPath("$.ratedBy").value(JwtTokens.EMAIL));

        UserEntity participant = users.findById(user.getId()).orElseThrow();
        changeRole(participant, "PARTICIPANT");
        entityManager.clear();

        mvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Incomplete description"))
                .andExpect(jsonPath("$[0].works[0].rate").value(87))
                .andExpect(jsonPath("$[0].works[0].isRated").value(true));
    }

    private void changeRole(UserEntity target, String role) {
        target.setRole(role);
        users.saveAndFlush(target);
        users.removeAllRoles(target.getId());
        users.assignRole(target.getId(), role);
    }
}
