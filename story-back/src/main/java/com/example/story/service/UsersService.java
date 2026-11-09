package com.example.story.service;

import com.example.story.dto.users.RequestUserDataDto;
import com.example.story.dto.users.RequestUserPasswordDto;
import com.example.story.dto.users.ResponseUserDataDto;
import com.example.story.entity.UserEntity;
import com.example.story.mapper.UserMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class UsersService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public ResponseUserDataDto obtainUserData() {
        ResponseUserDataDto dto = userMapper.entityToUserDataDto(getCurrentUser(userRepository));
        dto.setApplicationsCount(applicationRepository.countByUserId(dto.getUserId()).intValue());
        return dto;
    }

    public void updateUserData(RequestUserDataDto dto) throws RuntimeException {
        UserEntity user = getCurrentUser(userRepository);

        if (dto.getFirstName() != null) {
            if (dto.getFirstName().isEmpty()) throw new RuntimeException("Empty first name field");
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null) {
            if (dto.getLastName().isEmpty()) throw new RuntimeException("Empty last name field");
            user.setLastName(dto.getLastName());
        }

        userRepository.save(user);
    }

    public void updateUserPassword(RequestUserPasswordDto dto) throws RuntimeException {
        UserEntity user = getCurrentUser(userRepository);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) throw new RuntimeException("Wrong password");

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }
}
