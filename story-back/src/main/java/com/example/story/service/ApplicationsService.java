package com.example.story.service;

import com.example.story.dto.applications.*;
import com.example.story.dto.works.ResponseWorkDto;
import com.example.story.entity.*;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.mapper.ApplicationMapper;
import com.example.story.mapper.MemberMapper;
import com.example.story.mapper.WorkMapper;
import com.example.story.configuration.StoryProperties;
import com.example.story.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private final WorkMapper workMapper;
    private final StorageCleanupService storageCleanupService;
    private final EventService eventService;

    public List<ResponseApplicationDto> obtainAll() {
        UserEntity user = getCurrentUser(userRepository);
        List<ApplicationEntity> applications = applicationRepository.findByUserId(user.getId());

        List<ResponseApplicationDto> result  = new ArrayList<>();
        for (ApplicationEntity application : applications) {
            result.add(buildResponseDto(application));
        }
        return result;
    }

    public ResponseApplicationDto obtain(Long appId) {
        ApplicationEntity application = getApplicationAndCheckAccess(appId);
        return buildResponseDto(application);
    }

    private ResponseApplicationDto buildResponseDto(ApplicationEntity application) {
        ResponseApplicationDto dto = applicationMapper.entityToApplicationDto(application);
        dto.setWorksCount(workRepository.countByApplicationId(application.getId()));
        dto.setContestName(application.getContestEntity().getName());
        dto.setContestId(application.getContestEntity().getId());

        List<String> links = new ArrayList<>();
        List<ResponseWorkDto> works = new ArrayList<>();
        for (WorkEntity work : application.getWorkEntities()) {
            links.add("/works/" + work.getId() + "/file");
            works.add(workMapper.entityToWorkDto(work));
        }
        dto.setLinksToWorks(links);
        dto.setWorks(works);

        List<ResponseApplicationMemberDto> memberDtoList = new ArrayList<>();
        for (MemberEntity member : application.getMemberEntities()) {
            memberDtoList.add(memberMapper.entityToApplicationMemberDto(member));
        }
        dto.setMembers(memberDtoList);

        return dto;
    }

    public ResponseApplicationIdDto create(Long conId, RequestApplicationDto dto) {
        if (eventService.isApplicationsClosed()) {
            throw new IllegalStateException("Application submission is closed");
        }

        UserEntity user = getUserAndCheckConditions(conId, dto);

        ContestEntity contest = contestRepository.findById(conId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest not found"));

        if (Boolean.TRUE.equals(dto.getIsTeam())) {
            validateMembers(dto.getMembers(), contest);
            if (Boolean.FALSE.equals(dto.getIsCurator())) {
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

        if (Boolean.TRUE.equals(dto.getIsTeam())) {
            for (RequestApplicationMemberDto memberDto : dto.getMembers()) {
                MemberEntity member = memberMapper.entityFromApplicationMemberDto(memberDto);
                member.setApplicationEntity(savedApplication);
                memberRepository.save(member);
            }
        }

        return applicationMapper.entityToApplicationIdDto(savedApplication);
    }

    public void update(Long appId, RequestApplicationDto dto) {
        ensureApplicationsOpen();
        ApplicationEntity application = getApplicationAndCheckAccess(appId);
        ContestEntity contest = application.getContestEntity();

        if (dto.getIsTeam() != null && !dto.getIsTeam().equals(application.getIsTeam())) {
            throw new IllegalArgumentException("Application type cannot be changed");
        }
        if (dto.getIsCurator() != null && !dto.getIsCurator().equals(application.getIsCurator())) {
            throw new IllegalArgumentException("Curator flag cannot be changed");
        }

        if (dto.getName() != null) {
            if (dto.getName().length() > storyProperties.getApplication().getNameSize()) {
                throw new IllegalArgumentException("Application name length bigger than "
                        + storyProperties.getApplication().getNameSize() + " characters");
            }
            if (applicationRepository.existsByNameAndContestEntityIdAndIdNot(
                    dto.getName(), contest.getId(), appId)) {
                throw new IllegalArgumentException("Application with this name already exists");
            }
        }

        if (dto.getName() != null) application.setName(dto.getName());
        if (dto.getDescription() != null) application.setDescription(dto.getDescription());
        if (dto.getCity() != null) application.setCity(dto.getCity());
        if (dto.getOrganization() != null) application.setOrganization(dto.getOrganization());
        if (dto.getMentorName() != null) application.setMentor(dto.getMentorName());

        if (application.getIsTeam()) {
            if (dto.getMembers() != null) {
                validateMembers(dto.getMembers(), contest);
                memberRepository.deleteByApplicationId(appId);
                for (RequestApplicationMemberDto memberDto : dto.getMembers()) {
                    MemberEntity member = memberMapper.entityFromApplicationMemberDto(memberDto);
                    member.setApplicationEntity(application);
                    memberRepository.save(member);
                }
            }
        } else if (dto.getMembers() != null && !dto.getMembers().isEmpty()) {
            throw new IllegalArgumentException("Individual application cannot contain team members");
        }

        applicationRepository.save(application);
    }

    public void delete(Long appId) {
        ensureApplicationsOpen();
        ApplicationEntity entity = getApplicationAndCheckAccess(appId);

        for (MemberEntity member : entity.getMemberEntities()) {
            memberRepository.delete(member);
        }

        for (WorkEntity work : entity.getWorkEntities()) {
            storageCleanupService.schedule(work.getFileName());
            workRepository.delete(work);
        }

        applicationRepository.delete(entity);
    }

    // --- вспомогательные методы ---

    private ApplicationEntity getApplicationAndCheckAccess(Long appId) {
        UserEntity user = getCurrentUser(userRepository);
        return applicationRepository.findByIdAndUserEntityId(appId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
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
        if (dto.getName().length() > storyProperties.getApplication().getNameSize()) {
            throw new RuntimeException("Application name length bigger than " + storyProperties.getApplication().getNameSize() + " characters");
        }
        if (applicationRepository.findByNameAndContestId(dto.getName(), conId).isPresent()) {
            throw new RuntimeException("Application with this name is already exist");
        }
        return user;
    }

    private void validateMembers(List<RequestApplicationMemberDto> members, ContestEntity contest) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Team must have at least one new member");
        }
        Set<String> uniqueMembers = new HashSet<>();
        for (RequestApplicationMemberDto member : members) {
            if (member.getFirstName() == null || member.getFirstName().isBlank()
                    || member.getLastName() == null || member.getLastName().isBlank()
                    || member.getBirthDate() == null) {
                throw new IllegalArgumentException("Team member data is incomplete");
            }
            if (!validateAge(member.getBirthDate(), contest.getStartAge(), contest.getEndAge())) {
                throw new IllegalArgumentException("The member's age is not suitable for this nomination");
            }
            String memberKey = member.getFirstName().trim().toLowerCase(Locale.ROOT) + '\n'
                    + member.getLastName().trim().toLowerCase(Locale.ROOT) + '\n'
                    + member.getBirthDate();
            if (!uniqueMembers.add(memberKey)) {
                throw new IllegalArgumentException("Team contains duplicate members");
            }
        }
    }

    private void ensureApplicationsOpen() {
        if (eventService.isApplicationsClosed()) {
            throw new IllegalStateException("Application submission is closed");
        }
    }
}
