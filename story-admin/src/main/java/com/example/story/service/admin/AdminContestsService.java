package com.example.story.service.admin;

import com.example.story.dto.contests.RequestContestDto;
import com.example.story.dto.contests.ResponseContestDto;
import com.example.story.entity.ContestEntity;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.mapper.ContestMapper;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.story.security.AuthorizationRules.requireAuthority;
import static com.example.story.security.Permissions.CONTEST_MANAGE;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminContestsService {

    private final ContestRepository contestRepository;
    private final ApplicationRepository applicationRepository;
    private final ContestMapper contestMapper;

    @Transactional(readOnly = true)
    public List<ResponseContestDto> obtainAll() {
        requireAuthority(CONTEST_MANAGE);
        return contestRepository.findAll().stream()
                .map(contestMapper::entityToContestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseContestDto obtain(Long contestId) {
        requireAuthority(CONTEST_MANAGE);
        return contestMapper.entityToContestDto(findContest(contestId));
    }

    public ResponseContestDto create(RequestContestDto dto) {
        requireAuthority(CONTEST_MANAGE);
        String name = dto.getName().trim();
        if (contestRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Номинация с таким названием уже существует");
        }

        ContestEntity contest = contestMapper.entityFromContestDto(dto);
        contest.setName(name);
        contest.setDescription(dto.getDescription().trim());
        return contestMapper.entityToContestDto(contestRepository.save(contest));
    }

    public ResponseContestDto update(Long contestId, RequestContestDto dto) {
        requireAuthority(CONTEST_MANAGE);
        ContestEntity contest = findContest(contestId);
        String name = dto.getName().trim();
        contestRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(contestId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Номинация с таким названием уже существует");
                });

        contest.setName(name);
        contest.setDescription(dto.getDescription().trim());
        contest.setStartAge(dto.getStartAge());
        contest.setEndAge(dto.getEndAge());
        return contestMapper.entityToContestDto(contestRepository.save(contest));
    }

    public void delete(Long contestId) {
        requireAuthority(CONTEST_MANAGE);
        ContestEntity contest = findContest(contestId);
        if (applicationRepository.countByContestId(contestId) > 0) {
            throw new IllegalStateException("Нельзя удалить номинацию, к которой уже привязаны заявки");
        }
        contestRepository.delete(contest);
    }

    private ContestEntity findContest(Long contestId) {
        return contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Номинация не найдена"));
    }
}
