package com.example.story.service.admin;

import com.example.story.dto.applications.*;
import com.example.story.entity.*;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.mapper.ApplicationMapper;
import com.example.story.mapper.MemberMapper;
import com.example.story.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.APPLICATION_MODERATE;
import static com.example.story.security.Permissions.APPLICATION_READ_ANY;

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
        requireAuthority(APPLICATION_READ_ANY);

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
        requireAuthority(APPLICATION_READ_ANY);

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
            result.add(buildResponse(application));
        }
        return result;
    }

    public ResponseApplicationAdminDto obtainByAppId(Long appId) {
        requireAuthority(APPLICATION_READ_ANY);

        ApplicationEntity application = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));

        return buildResponse(application);
    }

    public ResponseApplicationAdminDto updateStatus(Long appId, RequestApplicationStatusDto dto) {
        UserEntity admin = getCurrentUser(userRepository);
        requireAuthority(APPLICATION_MODERATE);

        ApplicationEntity application = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String rejectionReason = dto.getRejectionReason() == null ? null : dto.getRejectionReason().trim();
        if (dto.getStatus() == ApplicationStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalArgumentException("Rejection reason is required for rejected applications");
        }

        application.setStatus(dto.getStatus().name());
        application.setRejectionReason(dto.getStatus() == ApplicationStatus.REJECTED ? rejectionReason : null);
        application.setReviewedBy(admin.getEmail());
        application.setReviewedAt(LocalDateTime.now());

        return buildResponse(applicationRepository.saveAndFlush(application));
    }

    private ResponseApplicationAdminDto buildResponse(ApplicationEntity application) {
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
