package com.example.story.service.admin;

import com.example.story.dto.users.RequestUserRolesDto;
import com.example.story.entity.UserEntity;
import com.example.story.mapper.UserMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUsersServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AdminUsersService service = new AdminUsersService(
            users, mock(ApplicationRepository.class), mock(UserMapper.class), passwords);

    private UserEntity admin;

    @BeforeEach
    void authenticateRoleManager() {
        admin = new UserEntity();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setPassword("encoded");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("role:manage"))));
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(passwords.matches("current-password", admin.getPassword())).thenReturn(true);
        when(users.findAllRoleNames()).thenReturn(List.of("ADMIN", "PARTICIPANT"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lastAdministratorCannotLoseAdminRole() {
        RequestUserRolesDto request = request(Set.of("PARTICIPANT"));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.findRoleNamesByUserId(1L)).thenReturn(List.of("ADMIN"));
        when(users.countUsersWithRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUserRoles(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last administrator");

        verify(users, never()).removeAllRoles(1L);
    }

    @Test
    void roleChangeUpdatesMappingCompatibilityRoleAndAudit() {
        UserEntity participant = new UserEntity();
        participant.setId(2L);
        RequestUserRolesDto request = request(Set.of("PARTICIPANT", "ADMIN"));
        when(users.findById(2L)).thenReturn(Optional.of(participant));
        when(users.findRoleNamesByUserId(2L)).thenReturn(List.of("PARTICIPANT"));

        service.updateUserRoles(2L, request);

        verify(users).removeAllRoles(2L);
        verify(users).assignRole(2L, "ADMIN");
        verify(users).assignRole(2L, "PARTICIPANT");
        verify(users).auditRoleChange(1L, 2L, "PARTICIPANT", "ADMIN,PARTICIPANT");
        verify(users).save(participant);
    }

    private RequestUserRolesDto request(Set<String> roles) {
        RequestUserRolesDto dto = new RequestUserRolesDto();
        dto.setCurrentPassword("current-password");
        dto.setRoles(roles);
        return dto;
    }
}
