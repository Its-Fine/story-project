package com.example.story.service.admin;

import com.example.story.configuration.StoryProperties;
import com.example.story.dto.applications.*;
import com.example.story.entity.*;
import com.example.story.mapper.ApplicationMapper;
import com.example.story.mapper.MemberMapper;
import com.example.story.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.example.story.utilities.Helpers.getCurrentUser;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminApplicationsService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final ApplicationMapper applicationMapper;
    private final MemberMapper memberMapper;


    public Long obtainCount(Long userId, Long conId) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        if (userId == null) {
            if (conId == null) return applicationRepository.count();
            else return applicationRepository.countByContestId(conId);
        }
        else {
            if (conId == null) return applicationRepository.countByUserId(userId);
            else return applicationRepository.countByUserIdAndContestId(userId, conId);
        }
    }

    public List<ResponseApplicationAdminDto> obtainAll(Long userId, Long conId, Long count, Long page) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        List<ApplicationEntity> applications;
        if (count == null) {
            if (userId == null) {
                if (conId == null) applications = applicationRepository.findAll();
                else applications = applicationRepository.findByContestId(conId);
            }
            else {
                if (conId == null) applications = applicationRepository.findByUserId(userId);
                else applications = applicationRepository.findByUserIdAndContestId(userId, conId);
            }
        }
        else {
            if (userId == null) {
                if (conId == null) applications = applicationRepository.findAllLimited(count, page == null ? 0 : page * count);
                else applications = applicationRepository.findByContestIdLimited(conId, count, page == null ? 0 : page * count);
            } else {
                if (conId == null) applications = applicationRepository.findByUserIdLimited(userId, count, page == null ? 0 : page * count);
                else applications = applicationRepository.findByUserIdAndContestIdLimited(userId, conId, count, page == null ? 0 : page * count);
            }
        }

        List<ResponseApplicationAdminDto> result = new ArrayList<>();
        for (ApplicationEntity application : applications) {
            ResponseApplicationAdminDto dto = applicationMapper.entityToApplicationAdminDto(application);
            dto.setWorksCount(workRepository.countByApplicationId(application.getId()));
            dto.setContestName(application.getContestEntity().getName());

            List<ResponseApplicationMemberDto> memberDtoList = new ArrayList<>();
            for (MemberEntity member : application.getMemberEntities()) {
                memberDtoList.add(memberMapper.entityToApplicationMemberDto(member));
            }
            dto.setMembers(memberDtoList);

            result.add(dto);
        }
        return result;
    }

    public ResponseApplicationAdminDto obtainByAppId(Long appId) {
        UserEntity admin = getCurrentUser(userRepository);
        if (!admin.getRole().equals(StoryProperties.Roles.ADMIN.toString())) throw new RuntimeException("You are not an administrator");

        ApplicationEntity application = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));

        ResponseApplicationAdminDto dto = applicationMapper.entityToApplicationAdminDto(application);
        dto.setWorksCount(workRepository.countByApplicationId(application.getId()));
        dto.setContestName(application.getContestEntity().getName());

        List<ResponseApplicationMemberDto> memberDtoList = new ArrayList<>();
        for (MemberEntity member : application.getMemberEntities()) {
            memberDtoList.add(memberMapper.entityToApplicationMemberDto(member));
        }
        dto.setMembers(memberDtoList);

        return dto;
    }
}
