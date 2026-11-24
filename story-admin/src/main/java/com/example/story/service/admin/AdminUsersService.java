package com.example.story.service.admin;

import com.example.story.dto.users.RequestUserAccessDto;
import com.example.story.dto.users.RequestUserBannedDto;
import com.example.story.dto.users.RequestUserPasswordDto;
import com.example.story.dto.users.RequestUserRolesDto;
import com.example.story.dto.users.ResponseUserDataAdminDto;
import com.example.story.entity.UserEntity;
import com.example.story.mapper.UserMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.USER_MANAGE;
import static com.example.story.security.Permissions.ROLE_MANAGE;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUsersService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<ResponseUserDataAdminDto> obtainAllUserData(String email, Long count, Long page) {
        requireAuthority(USER_MANAGE);

        List<UserEntity> users;
        if (count == null) {
            if (email == null) users = userRepository.findAll();
            else users = userRepository.findAlikeByEmail(email);
        }
        else {
            if (email == null) users = userRepository.findAllLimited(count, page == null ? 0 : page * count);
            else users = userRepository.findAlikeByEmailLimited(email, count, page == null ? 0 : page * count);
        }

        List<ResponseUserDataAdminDto> result = new ArrayList<>();
        for (UserEntity entity : users) {
            ResponseUserDataAdminDto dto = userMapper.entityToUserDataAdminDto(entity);
            dto.setRoles(userRepository.findRoleNamesByUserId(entity.getId()));
            dto.setApplicationsCount(applicationRepository.countByUserId(dto.getUserId()).intValue());
            result.add(dto);
        }
        return result;
    }

    public Long obtainCountUserData(String email) {
        requireAuthority(USER_MANAGE);

        if (email == null) return userRepository.count();
        else return userRepository.countAlikeByEmail(email);
    }

    public ResponseUserDataAdminDto obtainUserData(Long userId) {
        requireAuthority(USER_MANAGE);

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ResponseUserDataAdminDto dto = userMapper.entityToUserDataAdminDto(user);
        dto.setRoles(userRepository.findRoleNamesByUserId(user.getId()));
        return dto;
    }

    public void updateUserPassword(Long userId, RequestUserPasswordDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        requireAuthority(USER_MANAGE);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль администратора");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void updateUserAccess(Long userId, RequestUserAccessDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        requireAuthority(USER_MANAGE);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль администратора");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        boolean previousValue = Boolean.TRUE.equals(user.getClosedAccess());
        user.setClosedAccess(dto.getAccess());
        userRepository.save(user);
        userRepository.auditSecurityChange(
                admin.getId(), user.getId(), "closed_access",
                previousValue, Boolean.TRUE.equals(dto.getAccess()));
    }

    public void updateUserBanned(Long userId, RequestUserBannedDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        requireAuthority(USER_MANAGE);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль администратора");
        }
        if (admin.getId().equals(userId) && Boolean.TRUE.equals(dto.getBanned())) {
            throw new IllegalArgumentException("Администратор не может заблокировать собственную учётную запись");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        boolean previousValue = Boolean.TRUE.equals(user.getBanned());
        boolean nextValue = Boolean.TRUE.equals(dto.getBanned());
        if (previousValue == nextValue) return;

        user.setBanned(nextValue);
        userRepository.save(user);
        userRepository.auditSecurityChange(
                admin.getId(), user.getId(), "banned", previousValue, nextValue);
    }

    public void updateUserRoles(Long userId, RequestUserRolesDto dto) {
        requireAuthority(ROLE_MANAGE);
        UserEntity admin = getCurrentUser(userRepository);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль администратора");
        }

        UserEntity target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Set<String> requestedRoles = new LinkedHashSet<>();
        dto.getRoles().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .sorted()
                .forEach(requestedRoles::add);

        Set<String> knownRoles = new LinkedHashSet<>(userRepository.findAllRoleNames());
        if (!knownRoles.containsAll(requestedRoles)) {
            throw new IllegalArgumentException("Указана неизвестная роль");
        }

        List<String> previousRoles = userRepository.findRoleNamesByUserId(userId);
        if (previousRoles.contains("ADMIN")
                && !requestedRoles.contains("ADMIN")
                && userRepository.countUsersWithRole("ADMIN") <= 1) {
            throw new IllegalStateException("Нельзя снять роль у последнего администратора");
        }

        userRepository.removeAllRoles(userId);
        requestedRoles.forEach(role -> userRepository.assignRole(userId, role));

        String compatibilityRole = requestedRoles.contains("ADMIN") ? "ADMIN" : requestedRoles.iterator().next();
        target.setRole(compatibilityRole);
        userRepository.save(target);
        userRepository.auditRoleChange(
                admin.getId(), target.getId(),
                String.join(",", previousRoles), String.join(",", requestedRoles));
    }
}
