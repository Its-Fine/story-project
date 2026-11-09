package com.example.story.service.admin;

import com.example.story.configuration.StoryProperties;
import com.example.story.dto.users.RequestUserAccessDto;
import com.example.story.dto.users.RequestUserPasswordDto;
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

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUsersService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<ResponseUserDataAdminDto> obtainAllUserData(String email, Long count, Long page) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

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
            dto.setApplicationsCount(applicationRepository.countByUserId(dto.getUserId()).intValue());
            result.add(dto);
        }
        return result;
    }

    public Long obtainCountUserData(String email) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        if (email == null) return userRepository.count();
        else return userRepository.countAlikeByEmail(email);
    }

    public ResponseUserDataAdminDto obtainUserData(Long userId) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.entityToUserDataAdminDto(user);
    }

    public void updateUserPassword(Long userId, RequestUserPasswordDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) throw new RuntimeException("Wrong password");

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void updateUserAccess(Long userId, RequestUserAccessDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) throw new RuntimeException("Wrong password");

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setClosedAccess(dto.getAccess());
        userRepository.save(user);
    }
}
