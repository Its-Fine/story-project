package com.example.story.service;

import com.example.story.dto.applications.*;
import com.example.story.entity.*;
import com.example.story.mapper.ApplicationMapper;
import com.example.story.mapper.MemberMapper;
import com.example.story.configuration.StoryProperties;
import com.example.story.repository.*;

import com.example.story.service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.story.utilities.Helpers.getCurrentUser;
import static com.example.story.utilities.Helpers.validateAge;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationsService {

    private final ApplicationRepository applicationRepository;
    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;
    private final MemberRepository memberRepository;
    private final StoryProperties storyProperties;
    private final ApplicationMapper applicationMapper;
    private final MemberMapper memberMapper;
    private final MinioStorageService storageService;

    public List<ResponseApplicationDto> obtainAll() {
        UserEntity user = getCurrentUser(userRepository);
        List<ApplicationEntity> applications = applicationRepository.findByUserId(user.getId());

        List<ResponseApplicationDto> result  = new ArrayList<>();
        for (ApplicationEntity application : applications) {
            ResponseApplicationDto dto = applicationMapper.entityToApplicationDto(application);
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

    public ResponseApplicationDto obtain(Long appId) {
        ApplicationEntity application = getApplicationAndCheckAccess(appId);

        ResponseApplicationDto dto = applicationMapper.entityToApplicationDto(application);
        dto.setWorksCount(workRepository.countByApplicationId(application.getId()));
        dto.setContestName(application.getContestEntity().getName());

        List<ResponseApplicationMemberDto> memberDtoList = new ArrayList<>();
        for (MemberEntity member : application.getMemberEntities()) {
            memberDtoList.add(memberMapper.entityToApplicationMemberDto(member));
        }
        dto.setMembers(memberDtoList);

        return dto;
    }

    public ResponseApplicationIdDto create(Long conId, RequestApplicationDto dto) {
        UserEntity user = getUserAndCheckConditions(conId, dto);

        ContestEntity contest = contestRepository.findById(conId).orElseThrow(() -> new RuntimeException("Contest not found"));

        if (dto.getIsTeam()) {
            for (RequestApplicationMemberDto memberDto : dto.getMembers()) {
                if (!validateAge(memberDto.getBirthDate(), contest.getStartAge(), contest.getEndAge())) {
                    throw new RuntimeException("The member's age is not suitable for this nomination");
                }
            }
            if (!dto.getIsCurator()) {
                if (!validateAge(user.getBirthDate(), contest.getStartAge(), contest.getEndAge())) {
                    throw new RuntimeException("The member's age is not suitable for this nomination");
                }
            }
        }
        else {
            if (!validateAge(user.getBirthDate(), contest.getStartAge(), contest.getEndAge())) {
                throw new RuntimeException("The member's age is not suitable for this nomination");
            }
        }

        ApplicationEntity application = applicationMapper.entityFromApplicationDto(dto);
        application.setContestEntity(contest);
        application.setUserEntity(user);
        ApplicationEntity savedApplication = applicationRepository.save(application);

        if (dto.getIsTeam()) {
            for (RequestApplicationMemberDto memberDto : dto.getMembers()) {
                MemberEntity member = memberMapper.entityFromApplicationMemberDto(memberDto);
                member.setApplicationEntity(savedApplication);
                memberRepository.save(member);
            }
        }

        return applicationMapper.entityToApplicationIdDto(savedApplication);
    }

    public void update(Long appId, RequestApplicationDto dto) {
        ApplicationEntity application = getApplicationAndCheckAccess(appId);
        ContestEntity contest = application.getContestEntity();

        if (dto.getName() != null) application.setName(dto.getName());
        if (dto.getDescription() != null) application.setDescription(dto.getDescription());
        if (dto.getCity() != null) application.setCity(dto.getCity());
        if (dto.getOrganization() != null) application.setOrganization(dto.getOrganization());
        if (dto.getMentorName() != null) application.setMentor(dto.getMentorName());

        ApplicationEntity savedApplication = applicationRepository.save(application);

        if (application.getIsTeam()) {
            if (dto.getMembers().isEmpty()) throw new RuntimeException("Team must have at least one new member");
            else {
                memberRepository.deleteByApplicationId(appId);
                for (RequestApplicationMemberDto memberDto : dto.getMembers()) {
                    if (!validateAge(memberDto.getBirthDate(), contest.getStartAge(), contest.getEndAge())) {
                        throw new RuntimeException("The member's age is not suitable for this nomination");
                    }
                    MemberEntity member = memberMapper.entityFromApplicationMemberDto(memberDto);
                    member.setApplicationEntity(savedApplication);
                    memberRepository.save(member);
                }
            }
        }
    }

    public void delete(Long appId) {
        ApplicationEntity entity = getApplicationAndCheckAccess(appId);

        for (MemberEntity member : entity.getMemberEntities()) {
            memberRepository.delete(member);
        }

        for (WorkEntity work : entity.getWorkEntities()) {
            try {
                storageService.deleteFile(work.getFileName());
            } catch (IOException e) {
                System.out.println("Error while deleting: " + work.getFileName());
            }
            workRepository.delete(work);
        }

        applicationRepository.delete(entity);
    }

    // --- вспомогательные методы ---

    private ApplicationEntity getApplicationAndCheckAccess(Long appId) {
        ApplicationEntity app = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("Application not found"));
        UserEntity user = getCurrentUser(userRepository);
        if (!app.getUserEntity().getId().equals(user.getId())) {
            throw new RuntimeException("Application not found");
        }
        return app;
    }

    private UserEntity getUserAndCheckConditions(Long conId, RequestApplicationDto dto) {
        UserEntity user = getCurrentUser(userRepository);
        if (storyProperties.getApplication().getCount() > -1 &&
                applicationRepository.countByUserId(user.getId()) >= storyProperties.getApplication().getCount()) {
            throw new RuntimeException("User already has max applications");
        }
        if (storyProperties.getContest().getApplicationCount() > -1 &&
                applicationRepository.countByUserIdAndContestId(user.getId(), conId) >= storyProperties.getContest().getApplicationCount()) {
            throw new RuntimeException("User already has max applications for this contest");
        }
        if (dto.getName().length() > storyProperties.getWork().getNameSize()) {
            throw new RuntimeException("Application name length bigger than " + storyProperties.getApplication().getNameSize() + " characters");
        }
        if (applicationRepository.findByNameAndContestId(dto.getName(), conId).isPresent()) {
            throw new RuntimeException("Application with this name is already exist");
        }
        return user;
    }
}
