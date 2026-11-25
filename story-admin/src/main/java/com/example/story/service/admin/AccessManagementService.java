package com.example.story.service.admin;

import com.example.story.dto.access.RequestRoleDto;
import com.example.story.dto.access.ResponsePermissionDto;
import com.example.story.dto.access.ResponseRoleDto;
import com.example.story.entity.RoleEntity;
import com.example.story.entity.UserEntity;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.repository.PermissionRepository;
import com.example.story.repository.RoleRepository;
import com.example.story.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.ROLE_MANAGE;
import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessManagementService {

    private static final Set<String> SYSTEM_ROLES = Set.of("ADMIN", "PARTICIPANT");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ResponsePermissionDto> obtainPermissions() {
        requireAuthority(ROLE_MANAGE);
        return permissionRepository.findAll().stream()
                .map(permission -> ResponsePermissionDto.builder()
                        .name(permission.getName())
                        .description(permission.getDescription())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResponseRoleDto> obtainRoles() {
        requireAuthority(ROLE_MANAGE);
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    public ResponseRoleDto createRole(RequestRoleDto dto) {
        requireAuthority(ROLE_MANAGE);
        String roleName = normalizeRoleName(dto.getName());
        if (roleRepository.existsById(roleName)) {
            throw new IllegalArgumentException("Роль с таким названием уже существует");
        }
        Set<String> permissions = validatePermissions(dto.getPermissions());

        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        role.setDescription(dto.getDescription().trim());
        roleRepository.save(role);
        permissions.forEach(permission -> roleRepository.assignPermission(roleName, permission));
        audit(roleName, "CREATE", Set.of(), permissions);
        return toDto(role);
    }

    public ResponseRoleDto updateRole(String roleName, RequestRoleDto dto) {
        requireAuthority(ROLE_MANAGE);
        String normalizedName = normalizeRoleName(roleName);
        RoleEntity role = roleRepository.findById(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));
        if (!normalizeRoleName(dto.getName()).equals(normalizedName)) {
            throw new IllegalArgumentException("Имя существующей роли изменять нельзя");
        }
        Set<String> permissions = validatePermissions(dto.getPermissions());
        if (normalizedName.equals("ADMIN") && !permissions.contains(ROLE_MANAGE)) {
            throw new IllegalArgumentException("Роль ADMIN должна сохранять разрешение role:manage");
        }

        Set<String> previousPermissions = new LinkedHashSet<>(roleRepository.findPermissionNames(normalizedName));
        role.setDescription(dto.getDescription().trim());
        roleRepository.save(role);
        roleRepository.removePermissions(normalizedName);
        permissions.forEach(permission -> roleRepository.assignPermission(normalizedName, permission));
        audit(normalizedName, "UPDATE", previousPermissions, permissions);
        return toDto(role);
    }

    public void deleteRole(String roleName) {
        requireAuthority(ROLE_MANAGE);
        String normalizedName = normalizeRoleName(roleName);
        if (SYSTEM_ROLES.contains(normalizedName)) {
            throw new IllegalStateException("Системные роли ADMIN и PARTICIPANT удалять нельзя");
        }
        RoleEntity role = roleRepository.findById(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Роль не найдена"));
        if (roleRepository.countAssignedUsers(normalizedName) > 0) {
            throw new IllegalStateException("Нельзя удалить роль, пока она назначена пользователям");
        }
        Set<String> previousPermissions = new LinkedHashSet<>(roleRepository.findPermissionNames(normalizedName));
        roleRepository.delete(role);
        audit(normalizedName, "DELETE", previousPermissions, Set.of());
    }

    private Set<String> validatePermissions(Set<String> requestedPermissions) {
        Set<String> normalized = new LinkedHashSet<>();
        requestedPermissions.stream().map(String::trim).sorted().forEach(normalized::add);
        Set<String> known = new LinkedHashSet<>();
        permissionRepository.findAll().forEach(permission -> known.add(permission.getName()));
        if (!known.containsAll(normalized)) {
            throw new IllegalArgumentException("Указано неизвестное разрешение");
        }
        return normalized;
    }

    private String normalizeRoleName(String roleName) {
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private ResponseRoleDto toDto(RoleEntity role) {
        return ResponseRoleDto.builder()
                .name(role.getName())
                .description(role.getDescription())
                .permissions(roleRepository.findPermissionNames(role.getName()))
                .assignedUsers(roleRepository.countAssignedUsers(role.getName()))
                .system(SYSTEM_ROLES.contains(role.getName()))
                .build();
    }

    private void audit(String roleName, String action,
                       Set<String> previousPermissions, Set<String> newPermissions) {
        UserEntity actor = getCurrentUser(userRepository);
        roleRepository.auditDefinitionChange(
                actor.getId(), roleName, action,
                String.join(",", previousPermissions), String.join(",", newPermissions));
    }
}
