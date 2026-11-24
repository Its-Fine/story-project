package com.example.story.service.admin;

import com.example.story.dto.contests.RequestContestDto;
import com.example.story.entity.ContestEntity;
import com.example.story.mapper.ContestMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.ContestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminContestsServiceTest {

    private final ContestRepository contests = mock(ContestRepository.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final AdminContestsService service = new AdminContestsService(
            contests, applications, mock(ContestMapper.class));

    @BeforeEach
    void authenticateContestManager() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com", null,
                        List.of(new SimpleGrantedAuthority("contest:manage"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contestWithApplicationsCannotBeDeleted() {
        ContestEntity contest = new ContestEntity();
        contest.setId(3L);
        when(contests.findById(3L)).thenReturn(Optional.of(contest));
        when(applications.countByContestId(3L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("привязаны заявки");

        verify(contests, never()).delete(contest);
    }

    @Test
    void duplicateContestNameIsRejected() {
        RequestContestDto request = new RequestContestDto();
        request.setName("Design");
        when(contests.findByName("Design")).thenReturn(Optional.of(new ContestEntity()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("уже существует");
    }
}
