package com.example.story.service.admin;

import com.example.story.dto.access.RequestRoleDto;
import com.example.story.entity.PermissionEntity;
import com.example.story.entity.RoleEntity;
import com.example.story.repository.PermissionRepository;
import com.example.story.repository.RoleRepository;
import com.example.story.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessManagementServiceTest {

    private final RoleRepository roles = mock(RoleRepository.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AccessManagementService service = new AccessManagementService(roles, permissions, users);

    @BeforeEach
    void authenticateRoleManager() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com", null,
                        List.of(new SimpleGrantedAuthority("role:manage"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleCannotUseUnknownPermission() {
        RequestRoleDto request = request("CURATOR", Set.of("unknown:permission"));
        when(permissions.findAll()).thenReturn(List.of(permission("contest:read")));

        assertThatThrownBy(() -> service.createRole(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("неизвестное разрешение");

        verify(roles, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assignedCustomRoleCannotBeDeleted() {
        RoleEntity role = new RoleEntity();
        role.setName("CURATOR");
        when(roles.findById("CURATOR")).thenReturn(Optional.of(role));
        when(roles.countAssignedUsers("CURATOR")).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteRole("curator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("назначена пользователям");

        verify(roles, never()).delete(role);
    }

    private RequestRoleDto request(String name, Set<String> permissionNames) {
        RequestRoleDto dto = new RequestRoleDto();
        dto.setName(name);
        dto.setDescription("Test role");
        dto.setPermissions(permissionNames);
        return dto;
    }

    private PermissionEntity permission(String name) {
        PermissionEntity permission = new PermissionEntity();
        permission.setName(name);
        permission.setDescription(name);
        return permission;
    }
}
